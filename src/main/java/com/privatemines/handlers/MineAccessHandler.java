package com.privatemines.handlers;

import com.privatemines.PrivateMines;
import com.privatemines.models.MineRegion;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Handles mine access permissions and boundary validation
 */
public class MineAccessHandler {

    private final PrivateMines plugin;

    public MineAccessHandler(PrivateMines plugin) {
        this.plugin = plugin;
    }

    /**
     * Checks if a player can break a block at the given location
     * @param player The player attempting to break the block
     * @param location The location of the block
     * @return true if the player can break the block, false otherwise
     */
    public boolean canBreak(Player player, Location location) {
        UUID mineOwner = plugin.getMineManager().getMineOwner(location);
        if (mineOwner == null) {
            return true;
        }

        if (hasBreakPermission(player, mineOwner)) {
            return isBreakableLocation(mineOwner, location);
        }

        return false;
    }

    /**
     * Checks if player has permission to break blocks in this mine
     */
    private boolean hasBreakPermission(Player player, UUID mineOwner) {
        return mineOwner.equals(player.getUniqueId()) ||
                player.hasPermission("privatemines.bypass");
    }

    /**
     * Validates if the location is within breakable mine boundaries
     */
    private boolean isBreakableLocation(UUID mineOwner, Location location) {
        MineRegion region = plugin.getMineManager().getMineRegion(mineOwner);
        if (region == null) {
            return false;
        }

        return region.isInMineRegion(location);
    }

    public boolean canPlace(Player player, Location location) {
        UUID mineOwner = plugin.getMineManager().getMineOwner(location);
        if (mineOwner == null) return true;

        if (!mineOwner.equals(player.getUniqueId()) && !player.hasPermission("privatemines.bypass")) {
            return false;
        }

        MineRegion region = plugin.getMineManager().getMineRegion(mineOwner);
        return region != null && region.isInPlotArea(location);
    }

    /**
     * Checks if location is in a mine's mining area (for fake block breaks)
     */
    public boolean isInMiningArea(UUID mineOwner, Location location) {
        MineRegion region = plugin.getMineManager().getMineRegion(mineOwner);
        return region != null && region.isInMiningArea(location);
    }
}