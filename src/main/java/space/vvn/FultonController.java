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
import space.vvn.entityStorage.EntityStorageView;
import space.vvn.entityStorage.StoredEntity;
import space.vvn.entitySummoning.SummonableEntity;
import space.vvn.entitySummoning.SummonableRealEntity;
import space.vvn.entitySummoning.SummonableStoredEntity;

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
import org.bukkit.configuration.serialization.ConfigurationSerializable;
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
    @Getter private HashSet<Entity> fultoningEntities = new HashSet<Entity>(); // This could use some rework
    private HashMap<String, Queue<SummonableEntity>> dropPointEntityCache = new HashMap<String, Queue<SummonableEntity>>();

    public FultonController(JavaPlugin plugin) {
        this.plugin = plugin;
    }
    
    public void ScheduleFultonForEntity(Player player, Entity target, Block destination){
        if (destination == null){
            player.sendMessage("A Fulton Recovery destination has not been set.");
            return;
        }

        Fulton fulton = new Fulton(this, player, target, destination.getLocation(), this.plugin);
        if (fulton.Validate()){
            fulton.Start();
        }
    }

    public void SetHome(Player player, Block newHome){
        // Check if sky is unobstructed
        if (Utility.isBlockObstructed(newHome)){
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

    public List<DropPoint> getDropPoints(Player player){
        val config = this.plugin.getConfig();
        val settingPrefix = String.format("player.%s.drop-points.%s", player.getName(), player.getWorld().getName());
        //sendDebugMessage(player, settingPrefix);
        val dropPointConfigSection = config.getConfigurationSection(settingPrefix);
        Set<String> dropPoints = dropPointConfigSection.getKeys(false);

        List<DropPoint> result = new ArrayList<DropPoint>();
        for (String name : dropPoints){
            //sendDebugMessage(player, String.format("drop point: %s", name));
            Vector vec = dropPointConfigSection.getVector(name);
            Location loc = new Location(player.getWorld(), vec.getBlockX(), vec.getBlockY(), vec.getBlockZ());

            result.add(new DropPoint(name, loc, player));
        }

        return result;
    }

    private Queue<SummonableEntity> getCachedDropPointEntities(DropPoint point){
        final int radius = 10;
        Queue<SummonableEntity> entities;
        String cacheKey = String.format("%s_%s", point.getOwner().getName(), point.getName());
        if (!dropPointEntityCache.containsKey(cacheKey)){
            //sendDebugMessage(point.getOwner(), "creating new dropPointEntityCache");
            entities = new LinkedList<SummonableEntity>();
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
                entities.add(new SummonableRealEntity(e, this));
            }

            val entityStorage = new EntityStorageView(point.getOwner(), point.getOwner().getWorld(), this.plugin);

            for (val se : entityStorage.getStoredEntities()){
                entities.add(new SummonableStoredEntity((StoredEntity)se, this));
            }
        }
        return entities;
    }

    public SummonableEntity getEntityNearDropPoint(DropPoint point){
        val entities = getCachedDropPointEntities(point);

        if (entities.isEmpty()){
            return null;
        }
        return entities.peek();
    }

    public SummonableEntity getNextEntityNearDropPoint(DropPoint point){
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
}
