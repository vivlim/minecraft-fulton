package space.vvn.entitySummoning;

import org.bukkit.entity.Player;
import org.bukkit.Location;

public interface SummonableEntity {
    public boolean Summon(Player player, Location destination);
    public String getName();
    public String getOrigin();
}