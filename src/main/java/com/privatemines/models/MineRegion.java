package com.privatemines.models;

import org.bukkit.Location;
import org.bukkit.World;

public class MineRegion {

    private final World world;
    private final int minX, minY, minZ;
    private final int maxX, maxY, maxZ;
    private final Location spawnLocation;

    // Grass plot area bounds (calculated from spawn location)
    private final int grassMinX, grassMinY, grassMinZ;
    private final int grassMaxX, grassMaxY, grassMaxZ;

    public MineRegion(World world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, Location spawnLocation) {
        this.world = world;
        this.minX = Math.min(minX, maxX);
        this.minY = Math.min(minY, maxY);
        this.minZ = Math.min(minZ, maxZ);
        this.maxX = Math.max(minX, maxX);
        this.maxY = Math.max(minY, maxY);
        this.maxZ = Math.max(minZ, maxZ);
        this.spawnLocation = spawnLocation;

        // Calculate grass area using exact coordinates from your dev world
        // Spawn: 1088,106,1272
        // Grass corner 1: 1053,106,1345 (standing on top)
        // Grass corner 2: 1123,106,1275 (standing on top)
        int spawnX = spawnLocation.getBlockX();
        int spawnY = spawnLocation.getBlockY();
        int spawnZ = spawnLocation.getBlockZ();

        // Calculate grass area relative to spawn location
// Your schematic has grass blocks around the spawn area
        this.grassMinX = spawnX - 50;  // Larger area around spawn
        this.grassMinY = spawnY - 2;   // Ground level
        this.grassMinZ = spawnZ - 50;  // Square area around spawn
        this.grassMaxX = spawnX + 50;  // Larger area around spawn
        this.grassMaxY = spawnY + 50;  // 50 blocks above
        this.grassMaxZ = spawnZ + 50;  // Square area around spawn

    }

    public boolean isInMiningArea(Location location) {
        if (!location.getWorld().equals(world)) return false;

        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        // Mining area is between gold/emerald blocks
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    public boolean isInOverallMineArea(Location location) {
        if (!location.getWorld().equals(world)) return false;

        // Check if in either mining area OR plot area
        return isInMiningArea(location) || isInPlotArea(location);
    }

    public boolean isInPlotArea(Location location) {
        if (!location.getWorld().equals(world)) return false;

        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        // Plot area is above grass blocks (50 blocks high)
        return x >= grassMinX && x <= grassMaxX &&
                y > grassMinY && y <= grassMaxY &&
                z >= grassMinZ && z <= grassMaxZ;
    }

    public boolean isInMineRegion(Location location) {
        return isInMiningArea(location) || isInPlotArea(location);
    }

    // Getters
    public World getWorld() { return world; }
    public int getMinX() { return minX; }
    public int getMinY() { return minY; }
    public int getMinZ() { return minZ; }
    public int getMaxX() { return maxX; }
    public int getMaxY() { return maxY; }
    public int getMaxZ() { return maxZ; }
    public Location getSpawnLocation() { return spawnLocation; }
}