package space.vvn;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
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
    final int numSecondsBeforeReleaseTimeout = 4;

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
            target.setCustomName(String.format("%s", target.getName()));
            //target.setCustomNameVisible(true);
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
            player.sendMessage("Cannot recover between worlds.");
            return false;
        }

        // Check if sky is unobstructed
        if (Utility.isBlockObstructed(target.getWorld().getBlockAt(target.getLocation()))){
            player.sendMessage("Cannot recover without an unobstructed view of the sky above the target.");
            return false;
        }

        if (controller.getFultoningEntities().contains(target)){
            player.sendMessage("Entity already being recovered (or is stuck, maybe cannot reach the destination?)");
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

                val passengers = target.getPassengers();
                // If there are any passengers, kick them out before TP
                for (val passenger : passengers){
                    passenger.setGravity(false);
                    entity.removePassenger(passenger);
                }

                try {
                    entity.teleport(teleportDestination);
                    entity.setVelocity(new Vector(0, -3, 0));
                    scheduleSoftLanding(entity, destination, 1, 0);

                    for (val passenger : passengers){
                        passenger.setGravity(true);
                        entity.addPassenger(passenger);
                    }
                }
                catch (Exception e){
                    player.sendMessage("Something went wrong during recovery.");
                    System.out.println("Error during recovery");
                    System.out.println(e);
                    for (val passenger : passengers){
                        passenger.teleport(destination);
                        passenger.setGravity(true);
                    }
                }
            }
        }, delayServerTicks);
    }

    private void scheduleSoftLanding(Entity entity, Location destination, int ticksBetween, int iterations){
        this.plugin.getServer().getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable() {
            double distanceToEngageSoftLand = 3.5;
            double distanceToRelease = 1.8;
            public void run() {
                //debugPrintCoordinates(entity.getLocation(), "softLanding entity loc");
                if (!entity.isValid()){
                    player.sendMessage(String.format("'%s' could not be delivered to the drop point, and is being stored in stasis.", entity.getCustomName()));
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

                if (iterations % 5 == 0){
                    entity.getWorld().playEffect(entity.getLocation(), Effect.SMOKE, 0);
                }

                // if we aren't at the release distance, schedule this again
                // or if we're past the timeout for releasing. We should be near the ground by this point... something is probably just in the way
                if (distanceFromDestination > distanceToRelease && iterations < numSecondsBeforeReleaseTimeout * numTicksPerSecond){
                    scheduleSoftLanding(entity, destination, ticksBetween, iterations);
                }
                else {
                    entity.setGravity(true);
                    entity.setVelocity(new Vector(0, 0.1, 0));
                    controller.getFultoningEntities().remove(entity);

                    player.sendMessage(String.format("'%s' delivered successfully.", entity.getCustomName()));
                }
            }
        }, ticksBetween);
    }

}