package space.vvn;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

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

    public FultonController(JavaPlugin plugin) {
        this.plugin = plugin;
    }
    
    public void ScheduleFultonForEntity(Player player, Entity target, Block destination){
        if (destination == null){
            player.sendMessage("A Fulton Recovery destination has not been set.");
            return;
        }

        if (fultoningEntities.contains(target)){
            return;
        }
        fultoningEntities.add(target);

        // config
        int numSeconds = 4;
        int whenToYankSeconds = 3; // how many seconds in to start the yank.
        int numIterationsPerSecond = 8;
        Vector beforeYankVector = new Vector(0, 0.04, 0);
        Vector afterYankVector = new Vector(0, 6, 0);

        // details
        int numIterations = numSeconds * numIterationsPerSecond;
        int whenToYankIterations = whenToYankSeconds * numIterationsPerSecond;
        int numTicksPerSecond = 20;
        int numTicksPerIteration = numTicksPerSecond / numIterationsPerSecond;

        Location l = target.getLocation();

        if (target.getWorld() != destination.getWorld()){
            player.sendMessage("Cannot use Fulton Recovery between worlds.");
            return;
        }

        // Check if sky is unobstructed
        if (isBlockObstructed(target.getWorld().getBlockAt(l))){
            player.sendMessage("Cannot use Fulton Recovery without an unobstructed view of the sky above the recipient.");
            return;
        }

        // Assign a custom name so it doesn't get despawned
        if (target.getCustomName() == null){
            player.sendMessage("assigning custom name");
            target.setCustomName(String.format("Fulton-recovered %s", target.getName()));
            target.setCustomNameVisible(true);
        }

        target.setGravity(false);

        l.getWorld().playEffect(l, Effect.MOBSPAWNER_FLAMES, 31);
        for (int i = 0; i < numIterations; i++){
            if (i >= whenToYankIterations){
                scheduleFultonEffect(target, i * numTicksPerIteration);
                scheduleVelocityChange(target, afterYankVector, i * numTicksPerIteration);
            }
            else
            {
                scheduleVelocityChange(target, beforeYankVector, i * numTicksPerIteration);
            }
        }

        scheduleFultonDrop(target, destination.getLocation(), numTicksPerIteration * numIterations);
    }

    public void SetHome(Player player, Block newHome){
        // Check if sky is unobstructed
        if (isBlockObstructed(newHome)){
            player.sendMessage("Cannot set Fulton Recovery drop point without an unobstructed view of the sky.");
            return;
        }

        this.home = newHome;
    }

    private void scheduleVelocityChange(Entity entity, Vector vector, int delayServerTicks){
        // 20 ticks per second
        // one tick is about 0.05 seconds.
        // https://minecraft.gamepedia.com/Tick

        this.plugin.getServer().getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable() {
            public void run() {
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
                Location teleportDestination = new Location(destination.getWorld(), destination.getX(), 255, destination.getZ());
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

    private void sendDebugMessage(Entity anyEntity, String message){
        Player p = anyEntity.getWorld().getPlayers().get(0);
        p.sendMessage(message);
    }

    private void sendDebugMessage(Block anyEntity, String message){
        Player p = anyEntity.getWorld().getPlayers().get(0);
        p.sendMessage(message);
    }

    private void debugPrintCoordinates(Location coords, String label){
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
}
