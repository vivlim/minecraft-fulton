package space.vvn;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import lombok.Getter;
import lombok.val;
import lombok.var;
import space.vvn.entityStorage.EntityStorageView;

public class Fulton {
    @Getter private Player player;
    @Getter private Entity target;
    @Getter private Location destination;

    private FultonController controller;
    private JavaPlugin plugin;

    // config
    final int numSeconds = 4;
    final int whenToYankSeconds = 3; // how many seconds in to start the yank.
    final int numIterationsPerSecond = 8;
    final Vector beforeYankVector = new Vector(0, 0.04, 0);
    final Vector afterYankVector = new Vector(0, 6, 0);
    final EntityType balloonEntityType = EntityType.CHICKEN;

    // details
    final int numIterations = numSeconds * numIterationsPerSecond;
    final int whenToYankIterations = whenToYankSeconds * numIterationsPerSecond;
    final int numTicksPerSecond = 20;
    final int numTicksPerIteration = numTicksPerSecond / numIterationsPerSecond;

    public Fulton(FultonController controller, Player player, Entity target, Location destination, JavaPlugin plugin){
        this.controller = controller;
        this.player = player;
        this.target = target;
        this.destination = destination;
        this.plugin = plugin;
    }

    public void Start(){
        // this is the point of no return! we *have* to do something with this entity then remove it from fultoningEntities.
        controller.getFultoningEntities().add(target);

        // Assign a custom name so it doesn't get despawned
        if (target.getCustomName() == null){
            player.sendMessage("assigning custom name");
            target.setCustomName(String.format("Fulton-recovered %s", target.getName()));
            target.setCustomNameVisible(true);
        }

        target.setGravity(false);

        Entity balloon = target.getWorld().spawnEntity(target.getLocation().clone().add(0, 1.7, 0), balloonEntityType);
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
        if (Utility.isBlockObstructed(target.getWorld().getBlockAt(target.getLocation()))){
            player.sendMessage("Cannot use Fulton Recovery without an unobstructed view of the sky above the recipient.");
            return false;
        }

        if (controller.getFultoningEntities().contains(target)){
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
                    Utility.sendDebugMessage(entity, "entity not valid while going up");
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
                Utility.debugPrintCoordinates(entity.getLocation(), "pre-teleport-location");
                Utility.debugPrintCoordinates(teleportDestination, "teleportDestination");
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
                    Utility.sendDebugMessage(entity, "uh oh entity not valid");
                    new EntityStorageView(player, entity.getWorld(), plugin).storeEntity(entity);
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
                    controller.getFultoningEntities().remove(entity);
                }
            }
        }, ticksBetween);
    }

}