package com.privatemines.utils;

import org.bukkit.Location;

public class MineUtils {

    /**
     * Calculate mine size for levels 1-10 that fit within 197x197 mining area
     * Progressive scaling from 50x50 to 197x197
     */
    public static int calculateMineSize(int level) {
        switch (level) {
            case 1: return 50;   // 50x50 - starter size
            case 2: return 65;   // 65x65 - small expansion
            case 3: return 80;   // 80x80 - medium growth
            case 4: return 95;   // 95x95 - getting bigger
            case 5: return 110;  // 110x110 - halfway point
            case 6: return 125;  // 125x125 - substantial size
            case 7: return 140;  // 140x140 - large mine
            case 8: return 155;  // 155x155 - very large
            case 9: return 175;  // 175x175 - nearly max
            case 10: return 197; // 197x197 - maximum size (fits perfectly)
            default: return 50;  // fallback to level 1
        }
    }

    public static Location calculateGridPosition(int spacing, int startX, int startZ, int index) {
        int gridSize = 100;
        int x = (index % gridSize) * spacing + startX;
        int z = (index / gridSize) * spacing + startZ;
        return new Location(null, x, 64, z);
    }

    public static boolean isWithinBounds(Location location, Location min, Location max) {
        return location.getX() >= min.getX() && location.getX() <= max.getX() &&
                location.getY() >= min.getY() && location.getY() <= max.getY() &&
                location.getZ() >= min.getZ() && location.getZ() <= max.getZ();
    }

    public static double calculateDistance(Location loc1, Location loc2) {
        return Math.sqrt(Math.pow(loc1.getX() - loc2.getX(), 2) +
                Math.pow(loc1.getY() - loc2.getY(), 2) +
                Math.pow(loc1.getZ() - loc2.getZ(), 2));
    }
}