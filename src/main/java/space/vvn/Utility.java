package space.vvn;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class Utility {
    private static boolean debugMessagesOn = false;
    /**
     * Check if a block has any blocks above it.
     */
    public static boolean isBlockObstructed(Block target){

        Location targetLocation = target.getLocation();
        // Fetch the highest block at the target location, down by 1, because this will give you the air block above
        Location highestBlockAtTargetLocation = target.getWorld().getHighestBlockAt(target.getLocation()).getLocation();
        sendDebugMessage(target, String.format("target: %f %f %f. highest: %f %f %f", targetLocation.getX(), targetLocation.getY(), targetLocation.getZ(), highestBlockAtTargetLocation.getX(), highestBlockAtTargetLocation.getY(), highestBlockAtTargetLocation.getZ()));

        double distance = targetLocation.distance(highestBlockAtTargetLocation);

        sendDebugMessage(target, String.format("distance between blocks: %f", distance));

        // If we're further than two blocks, that's too far.
        return distance > 2d;
    }

    // nasty debug logging methods.
    public static void sendDebugMessage(Entity anyEntity, String message){
        if (!debugMessagesOn) { return; }
        Player p = anyEntity.getWorld().getPlayers().get(0);
        p.sendMessage(message);
    }

    public static void sendDebugMessage(Block anyEntity, String message){
        if (!debugMessagesOn) { return; }
        Player p = anyEntity.getWorld().getPlayers().get(0);
        p.sendMessage(message);
    }

    public static void debugPrintCoordinates(Location coords, String label){
        if (!debugMessagesOn) { return; }
        Player p = coords.getWorld().getPlayers().get(0);
        p.sendMessage(String.format("%s: %f %f %f", label, coords.getX(), coords.getY(), coords.getZ()));
    }
}