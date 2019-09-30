package space.vvn;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
/**
 * A location in a world where fulton-recovered entities are moved.
 */
public class DropPoint {
    @Getter private String name;
    @Getter private Location location;
    @Getter private Player owner;
}