package com.privatemines.managers;

import com.fastasyncworldedit.core.FaweAPI;
import com.privatemines.PrivateMines;
import com.privatemines.models.MineData;
import com.privatemines.models.MineRegion;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.function.pattern.RandomPattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockType;
import org.bukkit.Bukkit;
import com.privatemines.models.PlayerMine;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.world.block.BlockState;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import com.privatemines.utils.PacketUtils;
import com.privatemines.managers.WorldGuardManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class MineManager {

    private final PrivateMines plugin;
    private final Map<UUID, MineRegion> mineRegions;

    public MineManager(PrivateMines plugin) {
        this.plugin = plugin;
        this.mineRegions = new HashMap<>();
        loadExistingMines();
    }

    private void loadExistingMines() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            for (MineData mineData : plugin.getDataManager().getAllMines().values()) {
                createRegionForMine(mineData);
            }
        });
    }

    public CompletableFuture<Boolean> createMine(Player player) {
        if (plugin.getDataManager().hasMine(player.getUniqueId())) {
            return CompletableFuture.completedFuture(false);
        }

        Location location = plugin.getPoolManager().getNextLocation();

        return plugin.getSchematicManager().pasteSchematic(location)
                .thenApply(region -> {
                    if (region == null) return false;

                    // IMMEDIATELY register the region to fix protection timing
                    mineRegions.put(player.getUniqueId(), region);
                    plugin.getWorldGuardManager().createMineRegion(player.getUniqueId(), region);

                    ConfigManager config = plugin.getConfigManager();
                    MineData mineData = new MineData(
                            player.getUniqueId(),
                            config.getDefaultLevel(),
                            config.getDefaultBlocks(),
                            region.getSpawnLocation(),
                            player.getName()
                    );

                    plugin.getDataManager().setMineData(player.getUniqueId(), mineData);
                    fillMineWithBlocks(player.getUniqueId());
                    return true;
                });
    }

    public CompletableFuture<Void> resetMine(UUID playerUuid) {
        MineData mineData = plugin.getDataManager().getMineData(playerUuid);
        MineRegion region = mineRegions.get(playerUuid);
        if (mineData == null || region == null) {
            return CompletableFuture.completedFuture(null);
        }

        org.bukkit.entity.Player player = Bukkit.getPlayer(playerUuid);
        if (player != null && player.isOnline()) {
            // Teleport above the CENTER of mining area, not spawn
            int centerX = (region.getMinX() + region.getMaxX()) / 2;
            int centerZ = (region.getMinZ() + region.getMaxZ()) / 2;
            Location safeLocation = new Location(region.getWorld(), centerX + 0.5, region.getMaxY() + 5, centerZ + 0.5);

            player.teleport(safeLocation);
            player.sendMessage("§aTeleported above mining area - resetting...");
        }

        // Start filling blocks after teleport
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            fillMineWithBlocks(playerUuid);
            if (player != null && player.isOnline()) {
                player.sendMessage("§aMine reset complete!");
            }
        }, 10L); // 0.5 second delay

        return CompletableFuture.completedFuture(null);
    }

    public void deleteMine(UUID playerUuid) {
        MineData mineData = plugin.getDataManager().getMineData(playerUuid);
        if (mineData == null) return;

        MineRegion region = mineRegions.remove(playerUuid);
        if (region != null) {
            // Clear entire mine area
            for (int x = region.getMinX() - 10; x <= region.getMaxX() + 10; x++) {
                for (int y = region.getMinY() - 10; y <= region.getMaxY() + 10; y++) {
                    for (int z = region.getMinZ() - 10; z <= region.getMaxZ() + 10; z++) {
                        Location loc = new Location(region.getWorld(), x, y, z);
                        loc.getBlock().setType(Material.AIR);
                    }
                }
            }
            plugin.getPoolManager().returnLocation(mineData.getLocation());
        }

        plugin.getDataManager().deleteMineData(playerUuid);
    }

    public void setMineLevel(UUID playerUuid, int level) {
        MineData mineData = plugin.getDataManager().getMineData(playerUuid);
        if (mineData == null) return;

        int maxLevel = plugin.getConfigManager().getMaxLevel();
        if (level < 1 || level > maxLevel) return;

        mineData.setLevel(level);
        plugin.getDataManager().setMineData(playerUuid, mineData);

        // Immediately refill with new level size
        fillMineWithBlocks(playerUuid);

        plugin.getLogger().info("Updated mine level to " + level + " for " + mineData.getOwner());
    }

    public void setMineBlocks(UUID playerUuid, String blockIdentifier) {
        MineData mineData = plugin.getDataManager().getMineData(playerUuid);
        if (mineData == null) return;

        if (!plugin.getConfigManager().hasBlockConfig(blockIdentifier)) {
            plugin.getLogger().warning("Block config not found: " + blockIdentifier);
            return;
        }

        mineData.setBlockIdentifier(blockIdentifier);
        plugin.getDataManager().setMineData(playerUuid, mineData);

        // Immediately refill with new blocks
        fillMineWithBlocks(playerUuid);

        plugin.getLogger().info("Updated mine blocks to " + blockIdentifier + " for " + mineData.getOwner());
    }

    private void resizeMineRegion(UUID playerUuid, int level) {
        MineRegion currentRegion = mineRegions.get(playerUuid);
        if (currentRegion == null) return;

        int newSize = plugin.getConfigManager().getMineSize(level);
        Location center = currentRegion.getSpawnLocation();

        int halfSize = newSize / 2;
        MineRegion region = new MineRegion(
                center.getWorld(),
                center.getBlockX() - halfSize, center.getBlockY() - 10, center.getBlockZ() - halfSize,
                center.getBlockX() + halfSize, center.getBlockY() + 20, center.getBlockZ() + halfSize,
                center
        );

        mineRegions.put(playerUuid, region);
    }

    private void fillMineWithBlocks(UUID playerUuid) {
        MineData mineData = plugin.getDataManager().getMineData(playerUuid);
        MineRegion region = mineRegions.get(playerUuid);

        if (mineData == null || region == null) {
            plugin.getLogger().severe("Cannot fill mine - missing data");
            return;
        }

        Map<String, Object> blockConfig = plugin.getConfigManager().getBlockConfig(mineData.getBlockIdentifier());
        int mineLevel = mineData.getLevel();
        int mineSize = plugin.getConfigManager().getMineSize(mineLevel);
        int centerX = (region.getMinX() + region.getMaxX()) / 2;
        int centerZ = (region.getMinZ() + region.getMaxZ()) / 2;
        int halfSize = mineSize / 2;

        plugin.getLogger().info("FILLING MINE: Level " + mineLevel + " Size " + mineSize + " Center " + centerX + "," + centerZ);

        Bukkit.getScheduler().runTask(plugin, () -> {
            int filled = 0;
            for (int x = centerX - halfSize; x <= centerX + halfSize; x++) {
                for (int y = region.getMinY(); y <= region.getMaxY(); y++) {
                    for (int z = centerZ - halfSize; z <= centerZ + halfSize; z++) {
                        Location loc = new Location(region.getWorld(), x, y, z);
                        Material current = loc.getBlock().getType();

                        if (current != Material.SEA_LANTERN && current != Material.GRASS_BLOCK && current != Material.BEDROCK) {
                            Material newBlock = getRandomBlock(blockConfig);
                            loc.getBlock().setType(newBlock);
                            filled++;
                        }
                    }
                }
            }
            plugin.getLogger().info("FILLED " + filled + " BLOCKS FROM " + mineData.getBlockIdentifier());
        });
    }


    private void startGradualBlockSending(Player player, java.util.List<Location> locations,
                                          Map<String, Object> blockConfig, int currentIndex, int total) {
        if (!player.isOnline() || currentIndex >= locations.size()) {
            if (player.isOnline()) {
                player.sendMessage("§aMine loading complete! " + total + " blocks rendered");
            }
            return;
        }

        // Send 50 blocks per tick (very lag-friendly)
        int blocksPerTick = 50;
        int endIndex = Math.min(currentIndex + blocksPerTick, locations.size());

        for (int i = currentIndex; i < endIndex; i++) {
            Location loc = locations.get(i);
            Material fakeBlock = getRandomBlock(blockConfig);
            PacketUtils.sendBlockChange(player, loc, fakeBlock);
        }

        // Show progress
        int percentage = (endIndex * 100) / total;
        com.privatemines.utils.MessageUtils.sendActionBar(player, "§6Loading mine: " + percentage + "% complete");

        // Continue in next tick
        int nextIndex = endIndex;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            startGradualBlockSending(player, locations, blockConfig, nextIndex, total);
        }, 1L);
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

    private void createRegionForMine(MineData mineData) {
        int size = mineData.getSize();
        Location center = mineData.getLocation();
        int halfSize = size / 2;

        MineRegion region = new MineRegion(
                center.getWorld(),
                center.getBlockX() - halfSize, center.getBlockY() - 10, center.getBlockZ() - halfSize,
                center.getBlockX() + halfSize, center.getBlockY() + 20, center.getBlockZ() + halfSize,
                center
        );

        mineRegions.put(mineData.getUuid(), region);
    }

    public MineData getMineData(UUID playerUuid) {
        return plugin.getDataManager().getMineData(playerUuid);
    }

    public MineRegion getMineRegion(UUID playerUuid) {
        return mineRegions.get(playerUuid);
    }

    public boolean isInMine(Location location, UUID playerUuid) {
        MineRegion region = mineRegions.get(playerUuid);
        return region != null && region.isInMineRegion(location);
    }

    public UUID getMineOwner(Location location) {
        for (Map.Entry<UUID, MineRegion> entry : mineRegions.entrySet()) {
            MineRegion region = entry.getValue();
            if (region.isInOverallMineArea(location)) {
                return entry.getKey();
            }
        }
        return null;
    }

    // Track blocks mined for auto reset
    private final Map<UUID, Integer> blocksMinedCount = new HashMap<>();
    private final Map<UUID, Integer> totalBlocksInMine = new HashMap<>();

    public void trackBlockRemoval(Player player, Location location) {
        UUID playerUuid = player.getUniqueId();
        MineData mineData = getMineData(playerUuid);
        if (mineData == null) return;

        // Increment mined count
        int currentMined = blocksMinedCount.getOrDefault(playerUuid, 0) + 1;
        blocksMinedCount.put(playerUuid, currentMined);

        // Calculate total blocks if not cached
        if (!totalBlocksInMine.containsKey(playerUuid)) {
            int mineSize = plugin.getConfigManager().getMineSize(mineData.getLevel());
            MineRegion region = getMineRegion(playerUuid);
            int totalBlocks = mineSize * mineSize * (region.getMaxY() - region.getMinY() + 1);
            totalBlocksInMine.put(playerUuid, totalBlocks);
        }

        int totalBlocks = totalBlocksInMine.get(playerUuid);
        double percentageMined = (currentMined * 100.0) / totalBlocks;

        // Auto reset at 60% mined (40% remaining)
        if (percentageMined >= 60.0) {
            player.sendMessage("§eMine 60% depleted - auto resetting...");
            blocksMinedCount.put(playerUuid, 0); // Reset counter
            resetMine(playerUuid);
        }
    }
}