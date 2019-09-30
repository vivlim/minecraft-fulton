package space.vvn.entitySummoning;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.val;
import space.vvn.FultonController;
import space.vvn.entityStorage.StoredEntity;

@AllArgsConstructor
public class SummonableStoredEntity implements SummonableEntity {
    @Getter private StoredEntity storedEntity;
    @Getter private FultonController controller;

    @Override public String getName() {
        return storedEntity.getCustomName();
    }

    @Override public boolean Summon(Player player, Location destination){
        val me = destination.getWorld().spawnEntity(destination, storedEntity.getEntityType());
        me.setCustomName(storedEntity.getCustomName());
        return true;
    }
}