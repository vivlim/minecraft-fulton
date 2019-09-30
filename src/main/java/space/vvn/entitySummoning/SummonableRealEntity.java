package space.vvn.entitySummoning;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.val;
import space.vvn.FultonController;

@AllArgsConstructor
public class SummonableRealEntity implements SummonableEntity {
    @Getter private Entity entity;
    @Getter private FultonController controller;

    @Override public String getName() {
        return entity.getCustomName() == null ? entity.getName() : entity.getCustomName();
    }

    @Override public boolean Summon(Player player, Location destination){
        controller.ScheduleFultonForEntity(player, entity, entity.getWorld().getBlockAt(destination));
        return true; // todo: return bool from sched
    }
}