package space.vvn;

import java.util.logging.Level;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class Main extends JavaPlugin implements Listener {
	
	@EventHandler
	public void onLogin(PlayerJoinEvent event) {
		event.getPlayer().sendMessage("hello!");
	}

    @Override
    public void onEnable() {
		getServer().getPluginManager().registerEvents(this, this);
    }

}
