package com.privatemines.utils;

import com.privatemines.PrivateMines;
import com.privatemines.models.MineData;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.UUID;

public class MineBorderSystem {

    private final PrivateMines plugin;

    // Border offsets from spawn point (sea lantern) - CORRECTED
    private static final int NORTH_OFFSET = 195;  // +Z direction (was wrong - swapped with south)
    private static final int SOUTH_OFFSET = 352;  // -Z direction (was wrong - swapped with north)
    private static final int WEST_OFFSET = 169;   // -X direction
    private static final int EAST_OFFSET = 184;   // +X direction
    private static final int DOWN_OFFSET = 86;    // -Y direction (to -35)
    private static final int UP_OFFSET = 164;     // +Y direction (to 215)

    public MineBorderSystem(PrivateMines plugin) {
        this.plugin = plugin;
    }

    /**
     * Check if player is within their mine borders (handles visitors)
     * @param player The player to check
     * @param location The location to check
     * @return true if within borders, false if outside
     */
    public boolean isWithinMineBorders(Player player, Location location) {
        // Only check in mines world
        if (!location.getWorld().getName().equals(plugin.getConfigManager().getWorldName())) {
            return true; // Don't restrict other worlds
        }

        // Get effective mine owner (handles visitors)
        UUID effectiveOwner = plugin.getVisitorSystem().getEffectiveMineOwner(player.getUniqueId());
        MineData mineData = plugin.getMineManager().getMineData(effectiveOwner);

        if (mineData == null) {
            return false; // No mine = shouldn't be in mines world
        }

        Location spawnPoint = mineData.getLocation(); // This is the sea lantern location

        // Calculate border bounds
        int minX = spawnPoint.getBlockX() - WEST_OFFSET;
        int maxX = spawnPoint.getBlockX() + EAST_OFFSET;
        int minY = spawnPoint.getBlockY() - DOWN_OFFSET;
        int maxY = spawnPoint.getBlockY() + UP_OFFSET;
        int minZ = spawnPoint.getBlockZ() - SOUTH_OFFSET;
        int maxZ = spawnPoint.getBlockZ() + NORTH_OFFSET;

        // Check if player is within bounds
        return location.getBlockX() >= minX && location.getBlockX() <= maxX &&
                location.getBlockY() >= minY && location.getBlockY() <= maxY &&
                location.getBlockZ() >= minZ && location.getBlockZ() <= maxZ;
    }

    /**
     * Handle player going outside mine borders (handles visitors)
     * @param player The player who went outside
     */
    public void handleBorderViolation(Player player) {
        // Get effective mine owner (handles visitors)
        UUID effectiveOwner = plugin.getVisitorSystem().getEffectiveMineOwner(player.getUniqueId());
        MineData mineData = plugin.getMineManager().getMineData(effectiveOwner);

        if (mineData == null) {
            // No mine data, teleport to server spawn
            teleportToServerSpawn(player);
            return;
        }

        // ALWAYS teleport for reliability
        teleportToMineSpawn(player, mineData.getLocation());

        // Send warning message
        String message = plugin.getConfigManager().getMessage("border_warning");
        if (message != null && !message.isEmpty()) {
            player.sendMessage(message);
        }

        // Play sound effect
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);

        DebugUtils.debug(player, "Border violation handled for " + player.getName());
    }

    /**
     * Teleport player back to their mine spawn
     */
    private void teleportToMineSpawn(Player player, Location spawnLocation) {
        // Delay teleport slightly to prevent spam
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    player.teleport(spawnLocation);
                    DebugUtils.debug("Teleported " + player.getName() + " back to mine spawn");
                }
            }
        }.runTaskLater(plugin, 1L); // 1 tick delay
    }

    /**
     * Knockback player towards mine center
     */
    private void performKnockback(Player player, Location spawnLocation) {
        Location playerLoc = player.getLocation();

        // Calculate direction vector from player to spawn
        double deltaX = spawnLocation.getX() - playerLoc.getX();
        double deltaZ = spawnLocation.getZ() - playerLoc.getZ();

        // Normalize and apply knockback force
        double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        if (distance > 0) {
            double force = 2.0; // Knockback strength
            deltaX = (deltaX / distance) * force;
            deltaZ = (deltaZ / distance) * force;

            // Apply velocity
            player.setVelocity(player.getVelocity().add(new org.bukkit.util.Vector(deltaX, 0.5, deltaZ)));

            DebugUtils.debug("Applied knockback to " + player.getName() + " towards mine center");
        }
    }

    /**
     * Teleport to server spawn if no mine data
     */
    private void teleportToServerSpawn(Player player) {
        Location spawn = player.getWorld().getSpawnLocation();
        if (spawn != null) {
            player.teleport(spawn);
            player.sendMessage("§cYou don't have a mine! Teleported to spawn.");
            DebugUtils.debug("Teleported " + player.getName() + " to server spawn (no mine)");
        }
    }

    /**
     * Get border bounds for a specific mine (for visualization/debug)
     */
    public BorderBounds getBorderBounds(Location spawnPoint) {
        return new BorderBounds(
                spawnPoint.getBlockX() - WEST_OFFSET,   // minX
                spawnPoint.getBlockX() + EAST_OFFSET,   // maxX
                spawnPoint.getBlockY() - DOWN_OFFSET,   // minY
                spawnPoint.getBlockY() + UP_OFFSET,     // maxY
                spawnPoint.getBlockZ() - SOUTH_OFFSET,  // minZ
                spawnPoint.getBlockZ() + NORTH_OFFSET   // maxZ
        );
    }

    /**
     * Data class to hold border bounds
     */
    public static class BorderBounds {
        public final int minX, maxX, minY, maxY, minZ, maxZ;

        public BorderBounds(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
            this.minX = minX;
            this.maxX = maxX;
            this.minY = minY;
            this.maxY = maxY;
            this.minZ = minZ;
            this.maxZ = maxZ;
        }

        @Override
        public String toString() {
            return String.format("Border[X:%d to %d, Y:%d to %d, Z:%d to %d]",
                    minX, maxX, minY, maxY, minZ, maxZ);
        }
    }
}