package space.vvn;

import java.util.ArrayList;
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
    private Player player;

    public FultonController(JavaPlugin plugin) {
        this.plugin = plugin;
    }
    
    public void ScheduleFultonForEntity(Entity target){
        // config
        int numSeconds = 5;
        int whenToYankSeconds = 3; // how many seconds in to start the yank.
        int numIterationsPerSecond = 8;
        Vector beforeYankVector = new Vector(0, 0.1, 0);
        Vector afterYankVector = new Vector(0, 6, 0);

        // details
        int numIterations = numSeconds * numIterationsPerSecond;
        int whenToYankIterations = whenToYankSeconds * numIterationsPerSecond;
        int numTicksPerSecond = 20;
        int numTicksPerIteration = numTicksPerSecond / numIterationsPerSecond;

        Location l = target.getLocation();
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
    }

    private void scheduleVelocityChange(Entity entity, Vector vector, int delayServerTicks){
        // 20 ticks per second
        // one tick is about 0.05 seconds.
        // https://minecraft.gamepedia.com/Tick

        this.plugin.getServer().getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable() {
            public void run() {
                entity.setVelocity(vector);
                player.sendMessage(String.format("setting entity velocity to (%f, %f, %f)", vector.getX(), vector.getY(), vector.getZ()));
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
}
