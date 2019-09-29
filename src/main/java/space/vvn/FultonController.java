package space.vvn;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.val;
import lombok.var;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;

public class FultonController {

    private JavaPlugin plugin;
    public Block home; // todo: more sophisticated per-player home and persistence
    private HashSet<Entity> fultoningEntities = new HashSet<Entity>();
    private HashMap<String, Queue<Entity>> dropPointEntityCache = new HashMap<String, Queue<Entity>>();

    public FultonController(JavaPlugin plugin) {
        this.plugin = plugin;
    }
    
    public void ScheduleFultonForEntity(Player player, Entity target, Block destination){
        if (destination == null){
            player.sendMessage("A Fulton Recovery destination has not been set.");
            return;
        }

        Fulton fulton = new Fulton(player, target, destination.getLocation(), this.plugin);
        if (fulton.Validate()){
            fulton.Start();
        }
    }

    public void SetHome(Player player, Block newHome){
        // Check if sky is unobstructed
        if (isBlockObstructed(newHome)){
            player.sendMessage("Cannot set Fulton Recovery drop point without an unobstructed view of the sky.");
            return;
        }

        saveDropPoint(player, newHome.getLocation(), "default");
    }

    private void saveDropPoint(Player player, Location location, String dropPointName){
        val config = this.plugin.getConfig();
        val dropPointSetting = String.format("player.%s.drop-points.%s.%s", player.getName(), location.getWorld().getName(), dropPointName);
        config.set(dropPointSetting, location.toVector());
        this.plugin.saveConfig();
    }

    private void storeEntity(Player player, Entity entity){
        val config = this.plugin.getConfig();
        String dropPointSetting = String.format("player.%s.stored-entities.%s.%s", player.getName(), entity.getWorld().getName(), entity.getCustomName());
        config.set(dropPointSetting + ".id", entity.getEntityId());

        val count = config.getInt(dropPointSetting + ".count");
        config.set(dropPointSetting + ".count", count + 1);
        this.plugin.saveConfig();
    }

    public List<DropPoint> getDropPoints(Player player){
        val config = this.plugin.getConfig();
        val settingPrefix = String.format("player.%s.drop-points.%s", player.getName(), player.getWorld().getName());
        //sendDebugMessage(player, settingPrefix);
        val dropPointConfigSection = config.getConfigurationSection(settingPrefix);
        Set<String> dropPoints = dropPointConfigSection.getKeys(false);

        List<DropPoint> result = new ArrayList<DropPoint>();
        for (String name : dropPoints){
            //sendDebugMessage(player, String.format("drop point: %s", name));
            String dropPointSetting = String.format("%s.%s", settingPrefix, name);
            Vector vec = dropPointConfigSection.getVector(name);
            Location loc = new Location(player.getWorld(), vec.getBlockX(), vec.getBlockY(), vec.getBlockZ());

            result.add(new DropPoint(name, loc, player));
        }

        return result;
    }

    private Queue<Entity> getCachedDropPointEntities(DropPoint point){
        final int radius = 10;
        Queue<Entity> entities;
        String cacheKey = String.format("%s_%s", point.getOwner().getName(), point.getName());
        if (!dropPointEntityCache.containsKey(cacheKey)){
            //sendDebugMessage(point.getOwner(), "creating new dropPointEntityCache");
            entities = new LinkedList<Entity>();
            dropPointEntityCache.put(cacheKey, entities);
        }
        else {
            //sendDebugMessage(point.getOwner(), "reusing dropPointEntityCache");
            entities = dropPointEntityCache.get(cacheKey);
        }

        //sendDebugMessage(point.getOwner(), String.format("queue size: %d", entities.size()));
        if (entities.isEmpty()){
            // Okay, need to fetch entities from the drop point.
            //sendDebugMessage(point.getOwner(), String.format("Fetching entities"));

            val world = point.getLocation().getWorld();
            val nearEntities = world.getNearbyEntities(point.getLocation(), radius, radius, radius);
            //sendDebugMessage(point.getOwner(), String.format("got %d", nearEntities.size()));

            for (val e : nearEntities){
                entities.add(e);
            }
        }
        return entities;
    }

    public Entity getEntityNearDropPoint(DropPoint point){
        val entities = getCachedDropPointEntities(point);

        if (entities.isEmpty()){
            return null;
        }
        return entities.peek();
    }

    public Entity getNextEntityNearDropPoint(DropPoint point){
        var entities = getCachedDropPointEntities(point);

        if (entities.isEmpty()){
            return null;
        }
        entities.remove();

        if (entities.isEmpty()){
            // re-cache
            entities = getCachedDropPointEntities(point);
        }

        return entities.peek();
    }


    private static void sendDebugMessage(Entity anyEntity, String message){
        Player p = anyEntity.getWorld().getPlayers().get(0);
        p.sendMessage(message);
    }

    private static void sendDebugMessage(Block anyEntity, String message){
        Player p = anyEntity.getWorld().getPlayers().get(0);
        p.sendMessage(message);
    }

    private static void debugPrintCoordinates(Location coords, String label){
        Player p = coords.getWorld().getPlayers().get(0);
        p.sendMessage(String.format("%s: %f %f %f", label, coords.getX(), coords.getY(), coords.getZ()));
    }

    public boolean isBlockObstructed(Block target){

        Location targetLocation = target.getLocation();
        // Fetch the highest block at the target location, down by 1, because this will give you the air block above
        Location highestBlockAtTargetLocation = target.getWorld().getHighestBlockAt(target.getLocation()).getLocation();
        sendDebugMessage(target, String.format("target: %f %f %f. highest: %f %f %f", targetLocation.getX(), targetLocation.getY(), targetLocation.getZ(), highestBlockAtTargetLocation.getX(), highestBlockAtTargetLocation.getY(), highestBlockAtTargetLocation.getZ()));

        double distance = targetLocation.distance(highestBlockAtTargetLocation);

        sendDebugMessage(target, String.format("distance between blocks: %f", distance));

        // If we're further than two blocks, that's too far.
        return distance > 2d;
    }

    @AllArgsConstructor
    public class DropPoint {
        @Getter private String name;
        @Getter private Location location;
        @Getter private Player owner;
    }

    private class Fulton {
        @Getter private Player player;
        @Getter private Entity target;
        @Getter private Location destination;

        private Entity balloon;
        private JavaPlugin plugin;

        // config
        final int numSeconds = 4;
        final int whenToYankSeconds = 3; // how many seconds in to start the yank.
        final int numIterationsPerSecond = 8;
        final Vector beforeYankVector = new Vector(0, 0.04, 0);
        final Vector afterYankVector = new Vector(0, 6, 0);

        // details
        final int numIterations = numSeconds * numIterationsPerSecond;
        final int whenToYankIterations = whenToYankSeconds * numIterationsPerSecond;
        final int numTicksPerSecond = 20;
        final int numTicksPerIteration = numTicksPerSecond / numIterationsPerSecond;

        public Fulton(Player player, Entity target, Location destination, JavaPlugin plugin){
            this.player = player;
            this.target = target;
            this.destination = destination;
            this.plugin = plugin;
        }

        public void Start(){
            // this is the point of no return! we *have* to do something with this entity then remove it from fultoningEntities.
            fultoningEntities.add(target);

            // Assign a custom name so it doesn't get despawned
            if (target.getCustomName() == null){
                player.sendMessage("assigning custom name");
                target.setCustomName(String.format("Fulton-recovered %s", target.getName()));
                target.setCustomNameVisible(true);
            }

            target.setGravity(false);

            Entity balloon = target.getWorld().spawnEntity(target.getLocation().clone().add(0, 1.7, 0), EntityType.CHICKEN);
            balloon.setGravity(false);

            for (int i = 0; i < numIterations; i++){
                if (i >= whenToYankIterations){
                    scheduleFultonEffect(target, i * numTicksPerIteration);
                    scheduleVelocityChange(target, afterYankVector, i * numTicksPerIteration);
                }
                else
                {
                    scheduleVelocityChange(target, beforeYankVector, i * numTicksPerIteration);
                }

                if (i >= whenToYankIterations - (numIterationsPerSecond/3)){ // 1/3 of a second earlier, yank the balloon up.
                    scheduleVelocityChange(balloon, afterYankVector, i * numTicksPerIteration);
                }
                else
                {
                    scheduleVelocityChange(balloon, beforeYankVector, i * numTicksPerIteration);
                }
            }

            // remove the balloon after lifting is done.
            this.plugin.getServer().getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable() {
                public void run() {
                    balloon.remove();
                }
            }, numTicksPerIteration * numIterations);

            scheduleFultonDrop(target, destination, numTicksPerIteration * numIterations);
        }

        public boolean Validate(){
            if (target.getWorld() != destination.getWorld()){
                player.sendMessage("Cannot use Fulton Recovery between worlds.");
                return false;
            }

            // Check if sky is unobstructed
            if (isBlockObstructed(target.getWorld().getBlockAt(target.getLocation()))){
                player.sendMessage("Cannot use Fulton Recovery without an unobstructed view of the sky above the recipient.");
                return false;
            }

            if (fultoningEntities.contains(target)){
                player.sendMessage("Entity already being fultoned");
                return false;
            }

            return true;
        }


        private void scheduleVelocityChange(Entity entity, Vector vector, int delayServerTicks){
            // 20 ticks per second
            // one tick is about 0.05 seconds.
            // https://minecraft.gamepedia.com/Tick

            this.plugin.getServer().getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable() {
                public void run() {
                    if (!entity.isValid()){
                        sendDebugMessage(entity, "entity not valid while going up");
                        return;
                    }

                    entity.setVelocity(vector);
                }
            }, delayServerTicks);

        }

        private void scheduleFultonEffect(Entity entity, int delayServerTicks){
            this.plugin.getServer().getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable() {
                public void run() {
                    Location l = entity.getLocation();
                    l.getWorld().playEffect(l, Effect.SMOKE, 31);
                }
            }, delayServerTicks);

        }

        private void scheduleFultonDrop(Entity entity, Location destination, int delayServerTicks){
            this.plugin.getServer().getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable() {
                public void run() {
                    val dropHeight = Math.min(255, destination.getY() + 50);
                    Location teleportDestination = new Location(destination.getWorld(), destination.getX(), dropHeight, destination.getZ());
                    debugPrintCoordinates(entity.getLocation(), "pre-teleport-location");
                    debugPrintCoordinates(teleportDestination, "teleportDestination");
                    entity.teleport(teleportDestination);
                    entity.setVelocity(new Vector(0, -3, 0));
                    scheduleSoftLanding(entity, destination, 1);
                }
            }, delayServerTicks);
        }

        private void scheduleSoftLanding(Entity entity, Location destination, int ticksBetween){
            this.plugin.getServer().getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable() {
                double distanceToEngageSoftLand = 3.5;
                double distanceToRelease = 1.8;
                public void run() {
                    //debugPrintCoordinates(entity.getLocation(), "softLanding entity loc");
                    if (!entity.isValid()){
                        sendDebugMessage(entity, "uh oh entity not valid");
                        return;
                    }
                    double distanceFromDestination = entity.getLocation().getY() - destination.getY();
                    if (distanceFromDestination <= distanceToEngageSoftLand){
                        entity.setVelocity(new Vector(0, -0.06, 0));
                        entity.setFallDistance(0);
                    }
                    else
                    {
                        entity.setVelocity(new Vector(0, -3, 0));
                        entity.setFallDistance(0);
                    }

                    // if we aren't at the release distance, schedule this again
                    if (distanceFromDestination > distanceToRelease){
                        scheduleSoftLanding(entity, destination, ticksBetween);
                    }
                    else {
                        entity.setGravity(true);
                        entity.setVelocity(new Vector(0, 0.1, 0));
                        fultoningEntities.remove(entity);
                    }
                }
            }, ticksBetween);
        }

    }
}
