package space.vvn;

import java.util.List;
import java.util.logging.Level;
import org.bukkit.plugin.java.JavaPlugin;

import space.vvn.FultonController.DropPoint;

import org.bukkit.plugin.PluginManager;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;

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

        Entity target = event.getRightClicked();

        // Get default drop point for this player.
        List<DropPoint> dropPoints = controller.getDropPoints(p);

        if (dropPoints.size() == 0){
            p.sendMessage("No drop points set.");
            return;
        }

        DropPoint dp = dropPoints.get(0);

        controller.ScheduleFultonForEntity(p, target, p.getWorld().getBlockAt(dp.getLocation()));

    }
}
