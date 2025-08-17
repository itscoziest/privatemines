package com.privatemines.managers;

import com.privatemines.PrivateMines;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.Bukkit;
import com.privatemines.utils.OptimizedClearingSystem;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class PoolManager {

    private final PrivateMines plugin;
    private final Queue<Location> availableLocations;
    private final Set<Location> usedLocations;
    private int nextX;
    private int nextZ;

    private OptimizedClearingSystem clearingSystem;

    public PoolManager(PrivateMines plugin) {
        this.plugin = plugin;
        this.availableLocations = new LinkedList<>();
        this.usedLocations = new HashSet<>();
        this.nextX = plugin.getConfigManager().getStartX();
        this.nextZ = plugin.getConfigManager().getStartZ();
        this.clearingSystem = new OptimizedClearingSystem(plugin);

        plugin.getLogger().info("PoolManager initialized with " + getSpacing() + " block spacing from config");
    }

    // Get spacing from config instead of hardcoding
    private int getSpacing() {
        return plugin.getConfigManager().getWorldSpacing();
    }

    public Location getNextLocation() {
        // First check if we have any recycled locations available
        if (!availableLocations.isEmpty()) {
            Location recycledLocation = availableLocations.poll();
            usedLocations.add(recycledLocation);
            plugin.getLogger().info("Reusing recycled location: " + recycledLocation);
            return recycledLocation;
        }

        // No recycled locations available, generate a new one
        String worldName = plugin.getConfigManager().getWorldName();
        World world = plugin.getServer().getWorld(worldName);

        if (world == null) {
            plugin.getLogger().info("Creating new world: " + worldName);
            org.bukkit.WorldCreator creator = new org.bukkit.WorldCreator(worldName);
            creator.environment(org.bukkit.World.Environment.NORMAL);
            creator.type(org.bukkit.WorldType.FLAT);
            creator.generatorSettings("{\"layers\":[{\"block\":\"minecraft:air\",\"height\":1}],\"biome\":\"minecraft:the_void\",\"structures\":{\"structures\":{}}}");
            world = plugin.getServer().createWorld(creator);
        }

        // Use integer coordinates for consistency with storage
        Location newLocation = new Location(world, nextX, 64, nextZ);
        usedLocations.add(newLocation);

        plugin.getLogger().info("Generated new mine location: " + newLocation + " (Total used: " + usedLocations.size() + ")");

        calculateNextCoordinates();
        return newLocation;
    }

    private void calculateNextCoordinates() {
        int spacing = getSpacing(); // Use config value

        // Use the spacing from config
        nextX += spacing;

        // Create a grid pattern - when X gets too far, move to next Z row
        if (nextX > 10000) { // Limit X to 10,000 blocks
            nextX = plugin.getConfigManager().getStartX();
            nextZ += spacing;
        }

        plugin.getLogger().info("Next mine coordinates will be: (" + nextX + ", 64, " + nextZ + ") with spacing: " + spacing);
    }

    public void returnLocation(Location location) {
        if (location == null) return;

        // Normalize location for consistent comparison (remove decimal offsets)
        Location normalizedLocation = new Location(location.getWorld(),
                Math.floor(location.getX()),
                location.getY(),
                Math.floor(location.getZ()));

        // Check against normalized locations in usedLocations
        Location toRemove = null;
        for (Location used : usedLocations) {
            if (used.getWorld().equals(normalizedLocation.getWorld()) &&
                    Math.floor(used.getX()) == normalizedLocation.getBlockX() &&
                    used.getBlockY() == normalizedLocation.getBlockY() &&
                    Math.floor(used.getZ()) == normalizedLocation.getBlockZ()) {
                toRemove = used;
                break;
            }
        }

        if (toRemove != null) {
            usedLocations.remove(toRemove);
            availableLocations.offer(normalizedLocation);

            plugin.getLogger().info("Returned location to pool: " + normalizedLocation + " (Available: " + availableLocations.size() + ")");

            // Limit cached locations to prevent memory issues
            int maxCached = plugin.getConfigManager().getConfig().getInt("pool.max_cached", 100);
            while (availableLocations.size() > maxCached) {
                Location removed = availableLocations.poll();
                plugin.getLogger().info("Removed excess cached location: " + removed);
            }
        } else {
            plugin.getLogger().info("Location not found in used pool (this is normal): " + normalizedLocation);
        }
    }

    public boolean isLocationInUse(Location location) {
        return usedLocations.contains(location);
    }

    public int getUsedCount() {
        return usedLocations.size();
    }

    public int getAvailableCount() {
        return availableLocations.size();
    }

    public void clearLocationArea(Location location) {
        if (location == null || location.getWorld() == null) return;

        // Get proper paste origin (remove the +0.5 offset)
        Location pasteOrigin = new Location(location.getWorld(),
                Math.floor(location.getX()),
                location.getY() - 1,  // Go back to actual paste Y level
                Math.floor(location.getZ()));

        // Queue for safe clearing instead of immediate processing
        String playerName = "Unknown";
        clearingSystem.queueAreaClear(pasteOrigin, playerName);

        plugin.getLogger().info("Queued mine area for clearing: " + pasteOrigin);
    }

    /**
     * Queue clearing with player name for better logging
     */
    public void clearLocationArea(Location location, String playerName) {
        if (location == null || location.getWorld() == null) return;

        Location pasteOrigin = new Location(location.getWorld(),
                Math.floor(location.getX()),
                location.getY() - 1,
                Math.floor(location.getZ()));

        clearingSystem.queueAreaClear(pasteOrigin, playerName);
        plugin.getLogger().info("Queued mine clearing for " + playerName + " at " + pasteOrigin);
    }

    /**
     * Get spacing used between mines from config
     */
    public int getMinSpacing() {
        return getSpacing();
    }
}