package com.privatemines.utils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import java.io.File;

public class SchematicUtils {

    public static boolean isValidSchematicFile(File file) {
        if (!file.exists() || !file.isFile()) {
            return false;
        }

        String name = file.getName().toLowerCase();
        return name.endsWith(".schem") || name.endsWith(".schematic");
    }

    public static Location findMarkerBlock(World world, Location center, Material marker, int radius) {
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Location loc = center.clone().add(x, y, z);
                    if (loc.getBlock().getType() == marker) {
                        return loc;
                    }
                }
            }
        }
        return null;
    }

    public static boolean hasRequiredMarkers(World world, Location center, int radius) {
        boolean hasSpawn = findMarkerBlock(world, center, Material.SEA_LANTERN, radius) != null;
        boolean hasPlot = findMarkerBlock(world, center, Material.GRASS_BLOCK, radius) != null;
        boolean hasMin = findMarkerBlock(world, center, Material.GOLD_BLOCK, radius) != null;
        boolean hasMax = findMarkerBlock(world, center, Material.EMERALD_BLOCK, radius) != null;

        return hasSpawn && hasPlot && hasMin && hasMax;
    }

    public static void removeMarkerBlocks(World world, Location center, int radius) {
        Material[] markers = {Material.SEA_LANTERN, Material.GLOWSTONE, Material.GOLD_BLOCK, Material.EMERALD_BLOCK};

        for (Material marker : markers) {
            Location found = findMarkerBlock(world, center, marker, radius);
            if (found != null) {
                found.getBlock().setType(Material.AIR);
            }
        }
    }
}