package com.privatemines.utils;

import com.privatemines.PrivateMines;
import com.privatemines.models.MineData;
import com.privatemines.models.MineRegion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class OptimizedMineFillerSystem {

    private final PrivateMines plugin;
    private final Map<UUID, FillingTask> activeTasks = new HashMap<>();

    public OptimizedMineFillerSystem(PrivateMines plugin) {
        this.plugin = plugin;
    }

    /**
     * Fill mine optimized (only air blocks)
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
            player.sendMessage("§c⚠ Mine " + operation + " in progress... Please wait!");
            player.sendMessage("§eThis may take a moment depending on mine size.");
        }

        // Start filling task
        FillingTask task = new FillingTask(playerUuid, blockConfig,
                fillMinX, fillMaxX, region.getMinY(), region.getMaxY(), fillMinZ, fillMaxZ,
                region.getWorld(), operation);
        activeTasks.put(playerUuid, task);
        task.runTaskTimer(plugin, 1L, 1L);
    }

    private void cancelExistingTask(UUID playerUuid) {
        FillingTask existing = activeTasks.remove(playerUuid);
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
            // Target: 40 ticks (2 seconds) for completion
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
                    player.sendMessage("§6" + operation + " progress: " + Math.min(progress, 100) + "%");
                }
            }

            // Check completion
            if (currentX > maxX) {
                Player player = Bukkit.getPlayer(playerUuid);
                if (player != null && player.isOnline()) {
                    player.sendMessage("§aMine " + operation.toLowerCase() + " completed in " +
                            (this.getTaskId() / 20.0) + " seconds! (" + blocksPlaced + " blocks)");
                }

                activeTasks.remove(playerUuid);
                this.cancel();
            }
        }
    }
}