package com.privatemines.utils;

import org.bukkit.Location;

public class MineUtils {

    public static int calculateMineSize(int level) {
        switch (level) {
            case 1: return 50;
            case 2: return 75;
            case 3: return 100;
            case 4: return 150;
            case 5: return 200;
            case 6: return 250;
            case 7: return 300;
            case 8: return 350;
            case 9: return 400;
            case 10: return 500;
            default: return 50;
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