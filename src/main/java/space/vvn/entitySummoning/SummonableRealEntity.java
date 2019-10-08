package space.vvn.entitySummoning;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.val;
import space.vvn.DropPoint;
import space.vvn.FultonController;
import space.vvn.entityStorage.StoredEntity;

@AllArgsConstructor
public class SummonableRealEntity implements SummonableEntity {
    @Getter private Entity entity;
    @Getter private DropPoint fromDropPoint;
    @Getter private FultonController controller;


    @Override public String getName() {
        return entity.getCustomName() == null ? entity.getName() : entity.getCustomName();
    }

    @Override public String getOrigin() {
        return "drop point"; // todo; replace with name of drop point once there is more than one
    }

    @Override public boolean Summon(Player player, Location destination){
        if (!entity.isValid()){
            player.sendMessage(String.format("'%s' has despawned, attempting to recreate and send it.", entity.getCustomName()));
            return RecreateAndSummon(player, destination);
        }

        // Check distance from drop point and make sure this is still allowed.
        val distance = entity.getLocation().distance(fromDropPoint.getLocation());
        if (distance > DropPoint.entityRadiusFromDropPoint){
            player.sendMessage(String.format("Could not send '%s', it has moved too far from the drop point (distance: %f, max: %d)", entity.getCustomName(), distance, DropPoint.entityRadiusFromDropPoint));
            return false;
        }

        controller.ScheduleFultonForEntity(player, entity, entity.getWorld().getBlockAt(destination));
        return true; // todo: return bool from sched
    }

    private boolean RecreateAndSummon(Player player, Location destination){
        val storedEntity = new StoredEntity(entity);
        val summonableStoredEntity = new SummonableStoredEntity(storedEntity, controller);

        return summonableStoredEntity.Summon(player, destination);
    }
}