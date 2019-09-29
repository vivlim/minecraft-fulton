package space.vvn;

import java.util.List;
import java.util.logging.Level;
import org.bukkit.plugin.java.JavaPlugin;
import lombok.val;
import lombok.var;

import space.vvn.FultonController.DropPoint;

import org.bukkit.plugin.PluginManager;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;

public class Main extends JavaPlugin implements Listener {

    private FultonController controller;
    
    @EventHandler
    public void onLogin(PlayerJoinEvent event) {
        event.getPlayer().sendMessage("hello!");
    }

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        this.controller = new FultonController(this);
        getServer().getPluginManager().registerEvents(this, this);

        this.getCommand("fulton").setExecutor(new FultonCommand(controller));
        this.getCommand("fulton_home").setExecutor(new FultonHomeCommand(controller));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void rightClickEntity(PlayerInteractEntityEvent event){
        Player p = event.getPlayer();

        if (p.getInventory().getItemInMainHand().getType() != Material.STRING || !p.isSneaking()){
            return;
        }

        // don't take off-hand inputs
        if (event.getHand() != EquipmentSlot.HAND){
            // but do cancel them so we don't drop a torch or something
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        Entity target = event.getRightClicked();
        DropPoint dp = getDefaultDropPoint(p);

        controller.ScheduleFultonForEntity(p, target, p.getWorld().getBlockAt(dp.getLocation()));

    }

    @EventHandler(priority = EventPriority.HIGH)
    public void rightClick(PlayerInteractEvent event){
        Player p = event.getPlayer();
        if (p.getInventory().getItemInMainHand().getType() != Material.WOODEN_SWORD){
            return;
        }

        // don't take off-hand inputs
        if (event.getHand() != EquipmentSlot.HAND){
            // but do cancel them so we don't drop a torch or something
            event.setCancelled(true);
            return;
        }

        DropPoint dp = getDefaultDropPoint(p);

        if (p.isSneaking() && event.getAction() == Action.RIGHT_CLICK_BLOCK){
            event.setCancelled(true);
            p.sendMessage("sneak");
            val entity = controller.getEntityNearDropPoint(dp);
            p.sendMessage(String.format("Retrieving: %s from %s", entity.getCustomName() == null ? entity.getName() : entity.getCustomName(), dp.getName()));
            controller.ScheduleFultonForEntity(p, entity, p.getTargetBlock(null, 5));
        }
        else if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR){
            event.setCancelled(true);
            Entity entity = controller.getNextEntityNearDropPoint(dp);
            p.sendMessage(String.format("Selected: %s from %s", entity.getCustomName() == null ? entity.getName() : entity.getCustomName(), dp.getName()));
        }
    }

    private DropPoint getDefaultDropPoint(Player p){
        // Get default drop point for this player.
        List<DropPoint> dropPoints = controller.getDropPoints(p);

        if (dropPoints.size() == 0){
            p.sendMessage("No drop points set.");
            return null;
        }

        return dropPoints.get(0);
    }
}
