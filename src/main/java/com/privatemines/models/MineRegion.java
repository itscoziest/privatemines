package com.privatemines.models;

import org.bukkit.Location;
import org.bukkit.World;

public class MineRegion {

    private final World world;
    private final Location spawnLocation;

    // Mining area bounds (detected from Gold/Emerald blocks)
    private final int miningMinX, miningMinY, miningMinZ;
    private final int miningMaxX, miningMaxY, miningMaxZ;

    // Plot area bounds (detected from Lapis/Netherite blocks)
    private final int plotMinX, plotMinY, plotMinZ;
    private final int plotMaxX, plotMaxY, plotMaxZ;

    /**
     * Constructor for new mines with identifier block detection
     */
    public MineRegion(World world,
                      int miningMinX, int miningMinY, int miningMinZ,
                      int miningMaxX, int miningMaxY, int miningMaxZ,
                      int plotMinX, int plotMinY, int plotMinZ,
                      int plotMaxX, int plotMaxY, int plotMaxZ,
                      Location spawnLocation) {
        this.world = world;
        this.spawnLocation = spawnLocation;

        // Mining area bounds (from Gold/Emerald identifier blocks)
        this.miningMinX = Math.min(miningMinX, miningMaxX);
        this.miningMinY = Math.min(miningMinY, miningMaxY);
        this.miningMinZ = Math.min(miningMinZ, miningMaxZ);
        this.miningMaxX = Math.max(miningMinX, miningMaxX);
        this.miningMaxY = Math.max(miningMinY, miningMaxY);
        this.miningMaxZ = Math.max(miningMinZ, miningMaxZ);

        // Plot area bounds (from Lapis/Netherite identifier blocks)
        this.plotMinX = Math.min(plotMinX, plotMaxX);
        this.plotMinY = Math.min(plotMinY, plotMaxY);
        this.plotMinZ = Math.min(plotMinZ, plotMaxZ);
        this.plotMaxX = Math.max(plotMinX, plotMaxX);
        this.plotMaxY = Math.max(plotMinY, plotMaxY);
        this.plotMaxZ = Math.max(plotMinZ, plotMaxZ);
    }

    /**
     * Constructor for legacy mines (backwards compatibility)
     */
    public MineRegion(World world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, Location spawnLocation) {
        this.world = world;
        this.spawnLocation = spawnLocation;

        // For legacy mines, use the old bounds as mining area
        this.miningMinX = Math.min(minX, maxX);
        this.miningMinY = Math.min(minY, maxY);
        this.miningMinZ = Math.min(minZ, maxZ);
        this.miningMaxX = Math.max(minX, maxX);
        this.miningMaxY = Math.max(minY, maxY);
        this.miningMaxZ = Math.max(minZ, maxZ);

        // Calculate plot area using old logic for legacy compatibility
        int spawnX = spawnLocation.getBlockX();
        int spawnY = spawnLocation.getBlockY();
        int spawnZ = spawnLocation.getBlockZ();

        this.plotMinX = spawnX - 50;
        this.plotMinY = spawnY - 2;
        this.plotMinZ = spawnZ - 50;
        this.plotMaxX = spawnX + 50;
        this.plotMaxY = spawnY + 50;
        this.plotMaxZ = spawnZ + 50;
    }

    public boolean isInMiningArea(Location location) {
        if (!location.getWorld().equals(world)) return false;

        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        return x >= miningMinX && x <= miningMaxX &&
                y >= miningMinY && y <= miningMaxY &&
                z >= miningMinZ && z <= miningMaxZ;
    }

    public boolean isInPlotArea(Location location) {
        if (!location.getWorld().equals(world)) return false;

        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        return x >= plotMinX && x <= plotMaxX &&
                y >= plotMinY && y <= plotMaxY &&
                z >= plotMinZ && z <= plotMaxZ;
    }

    public boolean isInOverallMineArea(Location location) {
        if (!location.getWorld().equals(world)) return false;
        return isInMiningArea(location) || isInPlotArea(location);
    }

    public boolean isInMineRegion(Location location) {
        return isInOverallMineArea(location);
    }

    // Getters for mining area (backwards compatibility)
    public World getWorld() { return world; }
    public int getMinX() { return miningMinX; }
    public int getMinY() { return miningMinY; }
    public int getMinZ() { return miningMinZ; }
    public int getMaxX() { return miningMaxX; }
    public int getMaxY() { return miningMaxY; }
    public int getMaxZ() { return miningMaxZ; }
    public Location getSpawnLocation() { return spawnLocation; }

    // Getters for plot area
    public int getPlotMinX() { return plotMinX; }
    public int getPlotMinY() { return plotMinY; }
    public int getPlotMinZ() { return plotMinZ; }
    public int getPlotMaxX() { return plotMaxX; }
    public int getPlotMaxY() { return plotMaxY; }
    public int getPlotMaxZ() { return plotMaxZ; }

    // Debug method
    public void logBounds() {
        System.out.println("Mining Area: (" + miningMinX + "," + miningMinY + "," + miningMinZ +
                ") to (" + miningMaxX + "," + miningMaxY + "," + miningMaxZ + ")");
        System.out.println("Plot Area: (" + plotMinX + "," + plotMinY + "," + plotMinZ +
                ") to (" + plotMaxX + "," + plotMaxY + "," + plotMaxZ + ")");
    }
}