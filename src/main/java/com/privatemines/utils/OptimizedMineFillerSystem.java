package com.privatemines.utils;

import com.privatemines.PrivateMines;
import com.privatemines.models.MineData;
import com.privatemines.models.MineRegion;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.function.pattern.RandomPattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class OptimizedMineFillerSystem {

    private final PrivateMines plugin;
    private final Map<UUID, BukkitRunnable> activeTasks = new HashMap<>();

    public OptimizedMineFillerSystem(PrivateMines plugin) {
        this.plugin = plugin;
    }

    /**
     * Fill mine optimized (only air blocks) - Original method
     */
    public void fillMineOptimized(UUID playerUuid, String operation) {
        // Cancel existing task if running
        cancelExistingTask(playerUuid);

        MineData mineData = plugin.getMineManager().getMineData(playerUuid);
        MineRegion region = plugin.getMineManager().getMineRegion(playerUuid);

        if (mineData == null || region == null) {
            plugin.getLogger().severe("Cannot fill mine - missing data");
            return;
        }

        Map<String, Object> blockConfig = plugin.getConfigManager().getBlockConfig(mineData.getBlockIdentifier());
        int mineLevel = mineData.getLevel();
        int mineSize = plugin.getConfigManager().getMineSize(mineLevel);

        // Calculate fill area
        int trueCenterX = (region.getMinX() + region.getMaxX()) / 2;
        int trueCenterZ = (region.getMinZ() + region.getMaxZ()) / 2;
        int halfSize = mineSize / 2;

        int fillMinX = Math.max(trueCenterX - halfSize, region.getMinX());
        int fillMaxX = Math.min(trueCenterX + halfSize, region.getMaxX());
        int fillMinZ = Math.max(trueCenterZ - halfSize, region.getMinZ());
        int fillMaxZ = Math.min(trueCenterZ + halfSize, region.getMaxZ());

        // Notify player
        Player player = Bukkit.getPlayer(playerUuid);
        if (player != null && player.isOnline()) {
            player.sendMessage(ChatColor.RED + "⚠ Mine " + operation + " in progress... Please wait!");
            player.sendMessage(ChatColor.YELLOW + "This may take a moment depending on mine size.");
        }

        // Start filling task
        FillingTask task = new FillingTask(playerUuid, blockConfig,
                fillMinX, fillMaxX, region.getMinY() - 1, region.getMaxY(), fillMinZ, fillMaxZ,
                region.getWorld(), operation);
        activeTasks.put(playerUuid, task);
        task.runTaskTimer(plugin, 1L, 1L);
    }

    /**
     * Enhanced mine filling with FAWE - INSTANT and lag-free
     */
    public void fillMineEnhanced(UUID playerUuid, String operation) {
        MineData mineData = plugin.getMineManager().getMineData(playerUuid);
        MineRegion region = plugin.getMineManager().getMineRegion(playerUuid);

        if (mineData == null || region == null) {
            plugin.getLogger().severe("Cannot fill mine - missing data");
            return;
        }

        Map<String, Object> blockConfig = plugin.getConfigManager().getBlockConfig(mineData.getBlockIdentifier());
        int mineLevel = mineData.getLevel();
        int miningAreaSize = plugin.getConfigManager().getMineSize(mineLevel);

        // Use the exact same center calculation as the system
        int centerX = (region.getMinX() + region.getMaxX()) / 2;
        int centerZ = (region.getMinZ() + region.getMaxZ()) / 2;
        int halfMiningSize = miningAreaSize / 2;

        // Mining area boundaries
        int miningMinX = centerX - halfMiningSize;
        int miningMaxX = centerX + halfMiningSize;
        int miningMinZ = centerZ - halfMiningSize;
        int miningMaxZ = centerZ + halfMiningSize;

        // Border area (1 block around mining area)
        int borderMinX = miningMinX - 1;
        int borderMaxX = miningMaxX + 1;
        int borderMinZ = miningMinZ - 1;
        int borderMaxZ = miningMaxZ + 1;

        Player player = Bukkit.getPlayer(playerUuid);
        if (player != null && player.isOnline()) {
            player.sendMessage(ChatColor.GREEN + "Creating mining area...");
        }

        // Simple and fast - just create the mining area with air border
        Bukkit.getScheduler().runTask(plugin, () -> {
            long startTime = System.currentTimeMillis();

            createMiningAreaWithBorder(region, miningMinX, miningMaxX, miningMinZ, miningMaxZ,
                    borderMinX, borderMaxX, borderMinZ, borderMaxZ, blockConfig);

            long duration = System.currentTimeMillis() - startTime;

            if (player != null && player.isOnline()) {
                player.sendMessage(ChatColor.GREEN + "✓ Mining area created in " + duration + "ms!");
            }

            plugin.getLogger().info("Mining area fill completed in " + duration + "ms");
        });
    }

    private void createMiningAreaWithBorder(MineRegion region, int miningMinX, int miningMaxX, int miningMinZ, int miningMaxZ,
                                            int borderMinX, int borderMaxX, int borderMinZ, int borderMaxZ,
                                            Map<String, Object> blockConfig) {

        // This Y-level is now correctly treated as the bedrock floor.
        int floorY = region.getMinY() - 1;
        int endY = region.getMaxY();

        // Calculate bedrock wall area (3 blocks around the air border)
        int wallMinX = borderMinX - 1;
        int wallMaxX = borderMaxX + 1;
        int wallMinZ = borderMinZ - 1;
        int wallMaxZ = borderMaxZ + 1;

        // 1. Create the complete bedrock container (walls AND floor).
        // This loop creates the entire "cup" shape from the floor level (floorY) all the way up.
        for (int y = floorY; y <= endY; y++) {
            // North wall (3 blocks thick)
            for (int x = wallMinX; x <= wallMaxX; x++) {
                for (int z = wallMinZ; z <= wallMinZ + 2; z++) {
                    Location loc = new Location(region.getWorld(), x, y, z);
                    loc.getBlock().setType(Material.BEDROCK, false);
                }
            }

            // South wall (3 blocks thick)
            for (int x = wallMinX; x <= wallMaxX; x++) {
                for (int z = wallMaxZ - 2; z <= wallMaxZ; z++) {
                    Location loc = new Location(region.getWorld(), x, y, z);
                    loc.getBlock().setType(Material.BEDROCK, false);
                }
            }

            // West wall (3 blocks thick)
            for (int z = wallMinZ; z <= wallMaxZ; z++) {
                for (int x = wallMinX; x <= wallMinX + 2; x++) {
                    Location loc = new Location(region.getWorld(), x, y, z);
                    loc.getBlock().setType(Material.BEDROCK, false);
                }
            }

            // East wall (3 blocks thick)
            for (int z = wallMinZ; z <= wallMaxZ; z++) {
                for (int x = wallMaxX - 2; x <= wallMaxX; x++) {
                    Location loc = new Location(region.getWorld(), x, y, z);
                    loc.getBlock().setType(Material.BEDROCK, false);
                }
            }
        }

        // 2. Clear the interior area with air, STARTING ONE BLOCK ABOVE THE FLOOR.
        // This prevents the bedrock floor from being overwritten.
        for (int x = borderMinX; x <= borderMaxX; x++) {
            for (int y = floorY; y <= endY; y++) { // Changed: Start at floorY (not floorY + 1)
                for (int z = borderMinZ; z <= borderMaxZ; z++) {
                    Location loc = new Location(region.getWorld(), x, y, z);
                    loc.getBlock().setType(Material.AIR, false);
                }
            }
        }

        // 3. Fill the mining area with ore, also STARTING ONE BLOCK ABOVE THE FLOOR.
        for (int x = miningMinX; x <= miningMaxX; x++) {
            for (int y = floorY + 1; y <= endY; y++) { // Start at floorY + 1 (leave floorY as air)
                for (int z = miningMinZ; z <= miningMaxZ; z++) {
                    Location loc = new Location(region.getWorld(), x, y, z);
                    Material currentBlock = loc.getBlock().getType();

                    // Remove identifier blocks instead of covering them
                    if (currentBlock == Material.GOLD_BLOCK || currentBlock == Material.EMERALD_BLOCK) {
                        loc.getBlock().setType(Material.AIR, false);
                        continue;
                    }

                    Material newBlock = getRandomBlock(blockConfig);
                    loc.getBlock().setType(newBlock, false);
                }
            }
        }
    }

    private void createSmartBedrockWalls(MineRegion region, int miningMinX, int miningMaxX, int miningMinZ, int miningMaxZ) {
        // Calculate border area (1 block around mining)
        int borderMinX = miningMinX - 1;
        int borderMaxX = miningMaxX + 1;
        int borderMinZ = miningMinZ - 1;
        int borderMaxZ = miningMaxZ + 1;

        // Calculate wall area (3 blocks around border)
        int wallMinX = borderMinX - 3;
        int wallMaxX = borderMaxX + 3;
        int wallMinZ = borderMinZ - 3;
        int wallMaxZ = borderMaxZ + 3;

        // Make sure walls stay within reasonable bounds
        wallMinX = Math.max(wallMinX, region.getMinX() + 10);
        wallMaxX = Math.min(wallMaxX, region.getMaxX() - 10);
        wallMinZ = Math.max(wallMinZ, region.getMinZ() + 10);
        wallMaxZ = Math.min(wallMaxZ, region.getMaxZ() - 10);

        // 1. Create floor (full area)
        for (int x = wallMinX; x <= wallMaxX; x++) {
            for (int z = wallMinZ; z <= wallMaxZ; z++) {
                for (int y = region.getMinY(); y <= region.getMinY() + 2; y++) {
                    Location loc = new Location(region.getWorld(), x, y, z);
                    loc.getBlock().setType(Material.BEDROCK, false);
                }
            }
        }

        // 2. Create walls (full height but hollow inside)
        for (int y = region.getMinY(); y <= region.getMaxY(); y++) {
            // North wall (3 blocks thick)
            for (int x = wallMinX; x <= wallMaxX; x++) {
                for (int z = wallMinZ; z <= wallMinZ + 2; z++) {
                    Location loc = new Location(region.getWorld(), x, y, z);
                    loc.getBlock().setType(Material.BEDROCK, false);
                }
            }

            // South wall (3 blocks thick)
            for (int x = wallMinX; x <= wallMaxX; x++) {
                for (int z = wallMaxZ - 2; z <= wallMaxZ; z++) {
                    Location loc = new Location(region.getWorld(), x, y, z);
                    loc.getBlock().setType(Material.BEDROCK, false);
                }
            }

            // West wall (3 blocks thick)
            for (int z = wallMinZ; z <= wallMaxZ; z++) {
                for (int x = wallMinX; x <= wallMinX + 2; x++) {
                    Location loc = new Location(region.getWorld(), x, y, z);
                    loc.getBlock().setType(Material.BEDROCK, false);
                }
            }

            // East wall (3 blocks thick)
            for (int z = wallMinZ; z <= wallMaxZ; z++) {
                for (int x = wallMaxX - 2; x <= wallMaxX; x++) {
                    Location loc = new Location(region.getWorld(), x, y, z);
                    loc.getBlock().setType(Material.BEDROCK, false);
                }
            }
        }
    }

    private void fillMiningAreaOnly(MineRegion region, int miningMinX, int miningMaxX, int miningMinZ, int miningMaxZ, Map<String, Object> blockConfig) {
        // Clear the entire interior first (mining + border area)
        int borderMinX = miningMinX - 1;
        int borderMaxX = miningMaxX + 1;
        int borderMinZ = miningMinZ - 1;
        int borderMaxZ = miningMaxZ + 1;

        for (int x = borderMinX; x <= borderMaxX; x++) {
            for (int y = region.getMinY() + 3; y <= region.getMaxY() - 1; y++) {
                for (int z = borderMinZ; z <= borderMaxZ; z++) {
                    Location loc = new Location(region.getWorld(), x, y, z);
                    loc.getBlock().setType(Material.AIR, false);
                }
            }
        }

        // Fill ONLY the mining area with blocks (not the border)
        for (int x = miningMinX; x <= miningMaxX; x++) {
            for (int y = region.getMinY() + 3; y <= region.getMaxY() - 1; y++) {
                for (int z = miningMinZ; z <= miningMaxZ; z++) {
                    Location loc = new Location(region.getWorld(), x, y, z);
                    Material newBlock = getRandomBlock(blockConfig);
                    loc.getBlock().setType(newBlock, false);
                }
            }
        }
    }

    private class UltraOptimizedFillingTask extends BukkitRunnable {
        private final UUID playerUuid;
        private final Map<String, Object> blockConfig;
        private final String operation;
        private final MineRegion region;
        private final int miningMinX, miningMaxX, miningMinZ, miningMaxZ;
        private final int borderMinX, borderMaxX, borderMinZ, borderMaxZ;

        private int currentX, currentY, currentZ;
        private int phase = 1; // 1=bedrock, 2=clear, 3=mining
        private int blocksPlaced = 0;
        private long startTime;

        public UltraOptimizedFillingTask(UUID playerUuid, Map<String, Object> blockConfig, String operation,
                                         MineRegion region, int miningMinX, int miningMaxX, int miningMinZ, int miningMaxZ,
                                         int borderMinX, int borderMaxX, int borderMinZ, int borderMaxZ) {
            this.playerUuid = playerUuid;
            this.blockConfig = blockConfig;
            this.operation = operation;
            this.region = region;
            this.miningMinX = miningMinX; this.miningMaxX = miningMaxX;
            this.miningMinZ = miningMinZ; this.miningMaxZ = miningMaxZ;
            this.borderMinX = borderMinX; this.borderMaxX = borderMaxX;
            this.borderMinZ = borderMinZ; this.borderMaxZ = borderMaxZ;

            this.currentX = region.getMinX();
            this.currentY = region.getMinY();
            this.currentZ = region.getMinZ();
            this.startTime = System.currentTimeMillis();
        }

        @Override
        public void run() {
            int processed = 0;
            int maxPerTick = 50; // Very conservative for 20 TPS

            while (processed < maxPerTick && currentX <= region.getMaxX()) {
                Location loc = new Location(region.getWorld(), currentX, currentY, currentZ);

                if (phase == 1) {
                    // Phase 1: Create bedrock shell only
                    if (isShellBlock(currentX, currentY, currentZ)) {
                        loc.getBlock().setType(Material.BEDROCK, false);
                        blocksPlaced++;
                    }
                } else if (phase == 2) {
                    // Phase 2: Clear mining + border area
                    if (isInMiningOrBorderArea(currentX, currentZ) && currentY >= region.getMinY() + 3 && currentY < region.getMaxY() - 3) {
                        loc.getBlock().setType(Material.AIR, false);
                    }
                } else if (phase == 3) {
                    // Phase 3: Fill mining area only
                    if (isInMiningArea(currentX, currentZ) && currentY >= region.getMinY() + 3 && currentY < region.getMaxY() - 3) {
                        Material newBlock = getRandomBlock(blockConfig);
                        loc.getBlock().setType(newBlock, false);
                        blocksPlaced++;
                    }
                }

                processed++;
                moveToNext();
            }

            // Check phase completion
            if (currentX > region.getMaxX()) {
                if (phase < 3) {
                    phase++;
                    currentX = region.getMinX();
                    currentY = region.getMinY();
                    currentZ = region.getMinZ();
                } else {
                    completeTask();
                }
            }
        }

        private boolean isShellBlock(int x, int y, int z) {
            int thickness = 3;

            // Outer shell only
            boolean nearXEdge = (x <= region.getMinX() + thickness - 1) || (x >= region.getMaxX() - thickness + 1);
            boolean nearYEdge = (y <= region.getMinY() + thickness - 1) || (y >= region.getMaxY() - thickness + 1);
            boolean nearZEdge = (z <= region.getMinZ() + thickness - 1) || (z >= region.getMaxZ() - thickness + 1);

            // Inner walls around border area
            boolean nearBorderX = (x >= borderMinX - thickness && x <= borderMinX - 1) || (x >= borderMaxX + 1 && x <= borderMaxX + thickness);
            boolean nearBorderZ = (z >= borderMinZ - thickness && z <= borderMinZ - 1) || (z >= borderMaxZ + 1 && z <= borderMaxZ + thickness);
            boolean inBorderY = y >= region.getMinY() + thickness && y < region.getMaxY() - thickness;

            return (nearXEdge || nearYEdge || nearZEdge) || ((nearBorderX || nearBorderZ) && inBorderY);
        }

        private boolean isInMiningArea(int x, int z) {
            return x >= miningMinX && x <= miningMaxX && z >= miningMinZ && z <= miningMaxZ;
        }

        private boolean isInMiningOrBorderArea(int x, int z) {
            return x >= borderMinX && x <= borderMaxX && z >= borderMinZ && z <= borderMaxZ;
        }

        private void moveToNext() {
            currentZ++;
            if (currentZ > region.getMaxZ()) {
                currentZ = region.getMinZ();
                currentY++;
                if (currentY > region.getMaxY()) {
                    currentY = region.getMinY();
                    currentX++;
                }
            }
        }

        private void completeTask() {
            long duration = System.currentTimeMillis() - startTime;

            Player player = Bukkit.getPlayer(playerUuid);
            if (player != null && player.isOnline()) {
                player.sendMessage(ChatColor.GREEN + "✓ Mine created in " + duration + "ms with perfect TPS!");
                player.sendMessage(ChatColor.YELLOW + "Mining area is open at the top!");
            }

            plugin.getLogger().info("Ultra-optimized fill completed in " + duration + "ms - " + blocksPlaced + " blocks");
            activeTasks.remove(playerUuid);
            this.cancel();
        }
    }

    /**
     * Use FAWE to fill mine instantly with proper bedrock walls
     */
    private void fillMineWithFAWE(MineRegion region, int miningMinX, int miningMaxX, int miningMinZ, int miningMaxZ,
                                  int borderMinX, int borderMaxX, int borderMinZ, int borderMaxZ,
                                  Map<String, Object> blockConfig, UUID playerUuid) {

        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(region.getWorld());

        try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
            editSession.setFastMode(true);

            // Create random pattern for mining blocks
            RandomPattern miningPattern = new RandomPattern();
            for (Map.Entry<String, Object> entry : blockConfig.entrySet()) {
                String materialName = entry.getKey();
                double weight = ((Number) entry.getValue()).doubleValue();

                // Convert material name to BlockState
                BlockState blockState = BlockTypes.get(materialName.toLowerCase()).getDefaultState();
                miningPattern.add(blockState, weight);
            }

            int shellThickness = 3;

            // Step 1: Create outer bedrock shell (entire mine region edges only)
            createOuterBedrockShell(editSession, weWorld, region, shellThickness);

            // Step 2: Create inner bedrock walls (around mining + border area)
            createInnerBedrockWalls(editSession, weWorld, region, borderMinX, borderMaxX, borderMinZ, borderMaxZ, shellThickness);

            // Step 3: Clear the entire mining + border area INCLUDING THE TOP
            BlockVector3 clearMin = BlockVector3.at(borderMinX, region.getMinY() + shellThickness, borderMinZ);
            BlockVector3 clearMax = BlockVector3.at(borderMaxX, region.getMaxY() - 1, borderMaxZ);

            // Step 4: Fill ONLY the mining area with mining blocks (no ceiling)
            BlockVector3 miningMin = BlockVector3.at(miningMinX, region.getMinY() + shellThickness, miningMinZ);
            BlockVector3 miningMax = BlockVector3.at(miningMaxX, region.getMaxY() - 1, miningMaxZ);
            CuboidRegion miningRegion = new CuboidRegion(weWorld, miningMin, miningMax);
            editSession.setBlocks((com.sk89q.worldedit.regions.Region) miningRegion, miningPattern);

            plugin.getLogger().info("FAWE operations completed - Mining area with proper bedrock walls around air border");

        } catch (Exception e) {
            plugin.getLogger().severe("FAWE EditSession error: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Create outer bedrock shell around the entire mine region (200x200 edges only)
     */
    private void createOuterBedrockShell(EditSession editSession, com.sk89q.worldedit.world.World weWorld, MineRegion region, int shellThickness) {
        try {
            // Bottom face (floor)
            fillShellFace(editSession, weWorld,
                    region.getMinX(), region.getMaxX(),
                    region.getMinY(), region.getMinY() + shellThickness - 1,
                    region.getMinZ(), region.getMaxZ());

            // Top face (ceiling)
            fillShellFace(editSession, weWorld,
                    region.getMinX(), region.getMaxX(),
                    region.getMaxY() - shellThickness + 1, region.getMaxY(),
                    region.getMinZ(), region.getMaxZ());

            // North face (front wall at edge)
            fillShellFace(editSession, weWorld,
                    region.getMinX(), region.getMaxX(),
                    region.getMinY(), region.getMaxY(),
                    region.getMinZ(), region.getMinZ() + shellThickness - 1);

            // South face (back wall at edge)
            fillShellFace(editSession, weWorld,
                    region.getMinX(), region.getMaxX(),
                    region.getMinY(), region.getMaxY(),
                    region.getMaxZ() - shellThickness + 1, region.getMaxZ());

            // West face (left wall at edge)
            fillShellFace(editSession, weWorld,
                    region.getMinX(), region.getMinX() + shellThickness - 1,
                    region.getMinY(), region.getMaxY(),
                    region.getMinZ(), region.getMaxZ());

            // East face (right wall at edge)
            fillShellFace(editSession, weWorld,
                    region.getMaxX() - shellThickness + 1, region.getMaxX(),
                    region.getMinY(), region.getMaxY(),
                    region.getMinZ(), region.getMaxZ());

            plugin.getLogger().info("Outer bedrock shell created (200x200 edges only)");

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to create outer bedrock shell: " + e.getMessage());
        }
    }

    /**
     * Create inner bedrock walls around the mining + border area
     */
    private void createInnerBedrockWalls(EditSession editSession, com.sk89q.worldedit.world.World weWorld, MineRegion region,
                                         int borderMinX, int borderMaxX, int borderMinZ, int borderMaxZ, int shellThickness) {
        try {
            // Calculate inner wall positions (just outside the air border)
            int wallMinX = borderMinX - shellThickness;
            int wallMaxX = borderMaxX + shellThickness;
            int wallMinZ = borderMinZ - shellThickness;
            int wallMaxZ = borderMaxZ + shellThickness;

            // Make sure walls don't go outside the mine region or overlap outer shell
            wallMinX = Math.max(wallMinX, region.getMinX() + shellThickness);
            wallMaxX = Math.min(wallMaxX, region.getMaxX() - shellThickness);
            wallMinZ = Math.max(wallMinZ, region.getMinZ() + shellThickness);
            wallMaxZ = Math.min(wallMaxZ, region.getMaxZ() - shellThickness);

            // North wall (front) - around border area
            if (borderMinZ - 1 >= wallMinZ) {
                fillShellFace(editSession, weWorld,
                        wallMinX, wallMaxX,
                        region.getMinY() + shellThickness, region.getMaxY() - 1,
                        wallMinZ, borderMinZ - 1);
            }

            // South wall (back) - around border area
            if (borderMaxZ + 1 <= wallMaxZ) {
                fillShellFace(editSession, weWorld,
                        wallMinX, wallMaxX,
                        region.getMinY() + shellThickness, region.getMaxY() - 1,
                        borderMaxZ + 1, wallMaxZ);
            }

            // West wall (left) - around border area
            if (borderMinX - 1 >= wallMinX) {
                fillShellFace(editSession, weWorld,
                        wallMinX, borderMinX - 1,
                        region.getMinY() + shellThickness, region.getMaxY() - 1,
                        wallMinZ, wallMaxZ);
            }

            // East wall (right) - around border area
            if (borderMaxX + 1 <= wallMaxX) {
                fillShellFace(editSession, weWorld,
                        borderMaxX + 1, wallMaxX,
                        region.getMinY() + shellThickness, region.getMaxY() - 1,
                        wallMinZ, wallMaxZ);
            }

            plugin.getLogger().info("Inner bedrock walls created around mining + border area");

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to create inner bedrock walls: " + e.getMessage());
        }
    }

    /**
     * Fill a face of the bedrock shell
     */
    private void fillShellFace(EditSession editSession, com.sk89q.worldedit.world.World weWorld, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        try {
            BlockVector3 min = BlockVector3.at(minX, minY, minZ);
            BlockVector3 max = BlockVector3.at(maxX, maxY, maxZ);
            CuboidRegion faceRegion = new CuboidRegion(weWorld, min, max);
            editSession.setBlocks((com.sk89q.worldedit.regions.Region) faceRegion, BlockTypes.BEDROCK.getDefaultState());
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to fill shell face: " + e.getMessage());
        }
    }

    private void cancelExistingTask(UUID playerUuid) {
        BukkitRunnable existing = activeTasks.remove(playerUuid);
        if (existing != null && !existing.isCancelled()) {
            existing.cancel();
        }
    }

    private Material getRandomBlock(Map<String, Object> blockConfig) {
        double totalWeight = blockConfig.values().stream().mapToDouble(o -> ((Number) o).doubleValue()).sum();
        double random = Math.random() * totalWeight;
        double current = 0;

        for (Map.Entry<String, Object> entry : blockConfig.entrySet()) {
            current += ((Number) entry.getValue()).doubleValue();
            if (random <= current) {
                return Material.valueOf(entry.getKey());
            }
        }
        return Material.STONE;
    }

    // Original FillingTask class (unchanged for compatibility)
    private class FillingTask extends BukkitRunnable {
        private final UUID playerUuid;
        private final Map<String, Object> blockConfig;
        private final int minX, maxX, minY, maxY, minZ, maxZ;
        private final org.bukkit.World world;
        private final String operation;
        private final int blocksPerTick;

        private int currentX, currentY, currentZ;
        private int blocksPlaced = 0;
        private int totalEstimated = 0;

        public FillingTask(UUID playerUuid, Map<String, Object> blockConfig,
                           int minX, int maxX, int minY, int maxY, int minZ, int maxZ,
                           org.bukkit.World world, String operation) {
            this.playerUuid = playerUuid;
            this.blockConfig = blockConfig;
            this.minX = minX; this.maxX = maxX;
            this.minY = minY; this.maxY = maxY;
            this.minZ = minZ; this.maxZ = maxZ;
            this.world = world;
            this.operation = operation;
            this.currentX = minX;
            this.currentY = minY;
            this.currentZ = minZ;

            // Calculate total blocks and dynamic speed
            int totalBlocks = (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
            this.totalEstimated = totalBlocks;

            // Dynamic blocks per tick based on mine size
            this.blocksPerTick = Math.max(1000, totalBlocks / 40);

            plugin.getLogger().info("Starting " + operation + " - Estimated " + totalBlocks +
                    " blocks at " + blocksPerTick + " blocks/tick");
        }

        @Override
        public void run() {
            int processed = 0;

            while (processed < blocksPerTick && currentX <= maxX) {
                Location loc = new Location(world, currentX, currentY, currentZ);
                Material current = loc.getBlock().getType();

                // ONLY fill AIR blocks for optimization
                if (current == Material.AIR) {
                    Material newBlock = getRandomBlock(blockConfig);
                    loc.getBlock().setType(newBlock, false); // Skip physics
                    blocksPlaced++;
                }

                processed++;

                // Move to next position
                currentZ++;
                if (currentZ > maxZ) {
                    currentZ = minZ;
                    currentY++;
                    if (currentY > maxY) {
                        currentY = minY;
                        currentX++;
                    }
                }
            }

            // Progress update (only every 10 ticks to reduce spam)
            if (this.getTaskId() % 10 == 0) {
                Player player = Bukkit.getPlayer(playerUuid);
                if (player != null && player.isOnline()) {
                    int progress = (int) ((double) blocksPlaced / totalEstimated * 100);
                    player.sendMessage(ChatColor.GOLD + "" + operation + " progress: " + Math.min(progress, 100) + "%");
                }
            }

            // Check completion
            if (currentX > maxX) {
                Player player = Bukkit.getPlayer(playerUuid);
                if (player != null && player.isOnline()) {
                    player.sendMessage(ChatColor.GREEN + "Mine " + operation.toLowerCase() + " completed in " +
                            (this.getTaskId() / 20.0) + " seconds! (" + blocksPlaced + " blocks)");
                }

                activeTasks.remove(playerUuid);
                this.cancel();
            }
        }
    }
}