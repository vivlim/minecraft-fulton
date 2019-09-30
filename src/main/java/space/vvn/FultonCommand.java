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

public class FultonCommand implements CommandExecutor {

    private FultonController fulton;

    public FultonCommand(FultonController fulton) {
        this.fulton = fulton;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args){
        if (!(sender instanceof Player)){
            return false;
        }
        Player player = (Player) sender;

        List<Entity> entities = getEntitiesInLineOfSight(player);
        int numEntities = entities.size();
        player.sendMessage(String.format("%s entities in LOS", numEntities));

        if (numEntities == 0){
            player.sendMessage("no targets");
            return false;
        }

        Entity target = entities.get(0); // just take the first one. probably not optimal
        player.sendMessage(String.format("this command doesn't work rn and will probably just be removed"));
        //fulton.ScheduleFultonForEntity(player, target, fulton.home);

        return false;
    }

    private List<Entity> getEntitiesInLineOfSight(Player player){
        List<Entity> entitys = new ArrayList<Entity>();
        for(Entity e : player.getNearbyEntities(10, 10, 10)){
            if(e instanceof LivingEntity){
                if(isEntityLookedAt(player, (LivingEntity) e)){
                    entitys.add(e);
                }
            }
        }

        return entitys;
    }

    private boolean isEntityLookedAt(Player player, LivingEntity livingEntity){
        Location eye = player.getEyeLocation();
        Vector toEntity = livingEntity.getEyeLocation().toVector().subtract(eye.toVector());
        double dot = toEntity.normalize().dot(eye.getDirection());

        return dot > 0.90D;
    }
}
