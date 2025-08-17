package com.privatemines.utils;

import com.privatemines.PrivateMines;
import com.privatemines.models.MineData;
import com.privatemines.models.MineRegion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Map;
import java.util.UUID;

public class MineDataLoader {

    private final PrivateMines plugin;

    public MineDataLoader(PrivateMines plugin) {
        this.plugin = plugin;
    }

    /**
     * Load all existing mines after server start
     */
    public void loadAllMines() {
        plugin.getLogger().info("Loading existing mines...");

        Map<UUID, MineData> allMines = plugin.getDataManager().getAllMines();

        for (Map.Entry<UUID, MineData> entry : allMines.entrySet()) {
            UUID playerUuid = entry.getKey();
            MineData mineData = entry.getValue();

            loadSingleMine(playerUuid, mineData);
        }

        plugin.getLogger().info("Loaded " + allMines.size() + " existing mines");
    }

    /**
     * Load a single mine and create its region
     */
    public void loadSingleMine(UUID playerUuid, MineData mineData) {
        try {
            // Ensure world exists
            World world = ensureMinesWorldExists();
            if (world == null) {
                plugin.getLogger().severe("Failed to load mines world for player: " + mineData.getOwner());
                return;
            }

            // Fix location world if null
            Location mineLocation = mineData.getLocation();
            if (mineLocation.getWorld() == null) {
                mineLocation = new Location(world, mineLocation.getX(), mineLocation.getY(), mineLocation.getZ());
                mineData.setLocation(mineLocation); // Update the mine data
            }

            // Create region using exact same logic as new mines
            MineRegion region = createRegionFromLocation(mineLocation, world);

            if (region != null) {
                // Register the region
                plugin.getMineManager().registerMineRegion(playerUuid, region);

                // Create WorldGuard protection
                plugin.getWorldGuardManager().createMineRegion(playerUuid, region);

                plugin.getLogger().info("Successfully loaded mine for: " + mineData.getOwner());
            } else {
                plugin.getLogger().warning("Failed to create region for mine: " + mineData.getOwner());
            }

        } catch (Exception e) {
            plugin.getLogger().severe("Error loading mine for " + mineData.getOwner() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Create region from mine location using exact same offsets as schematic
     */
    private MineRegion createRegionFromLocation(Location pasteOrigin, World world) {
        // EXACT same offsets as SchematicManager
        final int GOLD_OFFSET_X = -99;
        final int GOLD_OFFSET_Y = -1;
        final int GOLD_OFFSET_Z = -74;

        final int EMERALD_OFFSET_X = 97;
        final int EMERALD_OFFSET_Y = -76;
        final int EMERALD_OFFSET_Z = -270;

        final int LAPIS_OFFSET_X = -35;
        final int LAPIS_OFFSET_Y = 0;
        final int LAPIS_OFFSET_Z = 73;

        final int NETHERITE_OFFSET_X = 35;
        final int NETHERITE_OFFSET_Y = 50;
        final int NETHERITE_OFFSET_Z = 3;

        // Calculate spawn location
        Location spawnLoc = new Location(world,
                pasteOrigin.getX() + 0.5,
                pasteOrigin.getY() + 1,
                pasteOrigin.getZ() + 0.5);

        // Calculate identifier positions
        Location goldLoc = pasteOrigin.clone().add(GOLD_OFFSET_X, GOLD_OFFSET_Y, GOLD_OFFSET_Z);
        Location emeraldLoc = pasteOrigin.clone().add(EMERALD_OFFSET_X, EMERALD_OFFSET_Y, EMERALD_OFFSET_Z);
        Location lapisLoc = pasteOrigin.clone().add(LAPIS_OFFSET_X, LAPIS_OFFSET_Y, LAPIS_OFFSET_Z);
        Location netheriteLoc = pasteOrigin.clone().add(NETHERITE_OFFSET_X, NETHERITE_OFFSET_Y, NETHERITE_OFFSET_Z);

        // Calculate bounds
        int miningMinX = Math.min(goldLoc.getBlockX(), emeraldLoc.getBlockX());
        int miningMinY = Math.min(goldLoc.getBlockY(), emeraldLoc.getBlockY());
        int miningMinZ = Math.min(goldLoc.getBlockZ(), emeraldLoc.getBlockZ());
        int miningMaxX = Math.max(goldLoc.getBlockX(), emeraldLoc.getBlockX());
        int miningMaxY = Math.max(goldLoc.getBlockY(), emeraldLoc.getBlockY());
        int miningMaxZ = Math.max(goldLoc.getBlockZ(), emeraldLoc.getBlockZ());

        int plotMinX = Math.min(lapisLoc.getBlockX(), netheriteLoc.getBlockX());
        int plotMinY = Math.min(lapisLoc.getBlockY(), netheriteLoc.getBlockY());
        int plotMinZ = Math.min(lapisLoc.getBlockZ(), netheriteLoc.getBlockZ());
        int plotMaxX = Math.max(lapisLoc.getBlockX(), netheriteLoc.getBlockX());
        int plotMaxY = Math.max(lapisLoc.getBlockY(), netheriteLoc.getBlockY());
        int plotMaxZ = Math.max(lapisLoc.getBlockZ(), netheriteLoc.getBlockZ());

        return new MineRegion(
                world,
                miningMinX, miningMinY, miningMinZ,
                miningMaxX, miningMaxY, miningMaxZ,
                plotMinX, plotMinY, plotMinZ,
                plotMaxX, plotMaxY, plotMaxZ,
                spawnLoc
        );
    }

    /**
     * Ensure mines world exists
     */
    private World ensureMinesWorldExists() {
        String worldName = plugin.getConfigManager().getWorldName();
        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            plugin.getLogger().info("Creating mines world: " + worldName);
            try {
                org.bukkit.WorldCreator creator = new org.bukkit.WorldCreator(worldName);
                creator.environment(org.bukkit.World.Environment.NORMAL);
                creator.type(org.bukkit.WorldType.FLAT);
                creator.generatorSettings("{\"layers\":[{\"block\":\"minecraft:air\",\"height\":1}],\"biome\":\"minecraft:the_void\",\"structures\":{\"structures\":{}}}");
                world = Bukkit.createWorld(creator);
                plugin.getLogger().info("Successfully created mines world!");
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to create mines world: " + e.getMessage());
            }
        }

        return world;
    }
}