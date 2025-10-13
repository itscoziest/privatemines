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
import com.privatemines.utils.MineDataLoader;
import com.privatemines.managers.WorldGuardManager;

import com.privatemines.utils.OptimizedMineFillerSystem;


import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class MineManager {

    private final PrivateMines plugin;
    private final Map<UUID, MineRegion> mineRegions;
    private MineDataLoader dataLoader;
    private OptimizedMineFillerSystem fillerSystem;

    public MineManager(PrivateMines plugin) {
        this.plugin = plugin;
        this.mineRegions = new HashMap<>();
        this.fillerSystem = new OptimizedMineFillerSystem(plugin);
        this.dataLoader = new MineDataLoader(plugin);

        // Load existing mines with new system
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            dataLoader.loadAllMines();
        }, 20L); // 1 second delay to ensure everything is initialized
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

        // Start filling blocks after teleport - USE forceReplace = true for reset
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            fillMineWithBlocks(playerUuid, true); // TRUE = only replace AIR blocks
            if (player != null && player.isOnline()) {
                player.sendMessage("§aMine reset complete!");
            }
        }, 10L); // 0.5 second delay

        return CompletableFuture.completedFuture(null);
    }

    public void deleteMine(UUID playerUuid) {
        MineData mineData = plugin.getDataManager().getMineData(playerUuid);
        if (mineData == null) {
            plugin.getLogger().warning("Tried to delete non-existent mine for player: " + playerUuid);
            return;
        }

        MineRegion region = mineRegions.remove(playerUuid);
        Location mineLocation = mineData.getLocation();

        plugin.getLogger().info("Deleting mine for player: " + mineData.getOwner());

        // DELETE WORLDGUARD REGION FIRST
        plugin.getWorldGuardManager().deleteMineRegion(playerUuid);

        // Teleport player out of mine if they're inside
        org.bukkit.entity.Player player = Bukkit.getPlayer(playerUuid);
        if (player != null && player.isOnline()) {
            if (player.getLocation().getWorld().getName().equals(plugin.getConfigManager().getWorldName())) {
                Location spawn = Bukkit.getWorlds().get(0).getSpawnLocation();
                player.teleport(spawn);
                player.sendMessage("§aMine deleted! You've been teleported to spawn.");
            }
        }

        // Clear mine area and return location
        if (mineLocation != null) {
            plugin.getPoolManager().clearLocationArea(mineLocation, mineData.getOwner());
            plugin.getPoolManager().returnLocation(mineLocation);
            plugin.getLogger().info("Mine location queued for clearing and returned to pool: " + mineLocation);
        }

        // Remove from data storage
        plugin.getDataManager().deleteMineData(playerUuid);

        // Clear tracking data
        blocksMinedCount.remove(playerUuid);
        totalBlocksInMine.remove(playerUuid);

        plugin.getLogger().info("Successfully deleted mine for player: " + mineData.getOwner());
    }

    public void setMineLevel(UUID playerUuid, int level) {
        MineData mineData = plugin.getDataManager().getMineData(playerUuid);
        if (mineData == null) return;

        int maxLevel = plugin.getConfigManager().getMaxLevel();
        if (level < 1 || level > maxLevel) return;

        mineData.setLevel(level);
        plugin.getDataManager().setMineData(playerUuid, mineData);

        // Immediately refill with new level size - FALSE = replace all mineable blocks
        fillMineWithBlocks(playerUuid, false);

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

        // Immediately refill with new blocks - FALSE = replace all mineable blocks
        fillMineWithBlocks(playerUuid, false);

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
        fillMineWithBlocks(playerUuid, false); // Default to not forcing replacement
    }

    private void fillMineWithBlocks(UUID playerUuid, boolean forceReplace) {
        // Use the enhanced filler system
        fillerSystem.fillMineEnhanced(playerUuid, forceReplace ? "RESET" : "FILL");
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

    /**
     * Creates region for existing mine data (server restart/reload)
     */
    private void createRegionForMine(MineData mineData) {
        plugin.getLogger().info("Loading existing mine for " + mineData.getOwner() + " at " + mineData.getLocation());

        // Use the same calculation method as SchematicManager
        Location pasteOrigin = mineData.getLocation();
        MineRegion region = calculateRegionsFromMineData(pasteOrigin);

        if (region != null) {
            mineRegions.put(mineData.getUuid(), region);
            plugin.getLogger().info("Successfully loaded mine region for " + mineData.getOwner());

            // Create WorldGuard region
            plugin.getWorldGuardManager().createMineRegion(mineData.getUuid(), region);
        } else {
            plugin.getLogger().warning("Failed to load mine region for " + mineData.getOwner());
        }
    }

    private MineRegion calculateRegionsFromMineData(Location pasteOrigin) {
        // Use exact same offsets as SchematicManager
        final int SPAWN_OFFSET_X = 0;
        final int SPAWN_OFFSET_Y = 0;
        final int SPAWN_OFFSET_Z = 0;

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

        // Calculate positions
        Location spawnLoc = pasteOrigin.clone().add(SPAWN_OFFSET_X + 0.5, SPAWN_OFFSET_Y + 1, SPAWN_OFFSET_Z + 0.5);
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
                pasteOrigin.getWorld(),
                miningMinX, miningMinY, miningMinZ,
                miningMaxX, miningMaxY, miningMaxZ,
                plotMinX, plotMinY, plotMinZ,
                plotMaxX, plotMaxY, plotMaxZ,
                spawnLoc
        );
    }


    /**
     * Public method to load a specific player's mine region
     * Called when player joins and region needs to be loaded
     */
    public void loadPlayerMineRegion(UUID playerUuid, MineData mineData) {
        if (mineRegions.containsKey(playerUuid)) {
            plugin.getLogger().info("Mine region already loaded for " + mineData.getOwner());
            return;
        }

        plugin.getLogger().info("Loading mine region for " + mineData.getOwner());

        // Calculate regions using the same method as createRegionForMine
        Location pasteOrigin = mineData.getLocation();
        MineRegion region = calculateRegionsFromMineData(pasteOrigin);

        if (region != null) {
            mineRegions.put(playerUuid, region);
            plugin.getLogger().info("Successfully loaded mine region for " + mineData.getOwner());

            // Create WorldGuard region if needed
            plugin.getWorldGuardManager().createMineRegion(playerUuid, region);
        } else {
            plugin.getLogger().warning("Failed to load mine region for " + mineData.getOwner());
        }
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
    public void registerMineRegion(UUID playerUuid, MineRegion region) {
        mineRegions.put(playerUuid, region);
    }

    public MineDataLoader getDataLoader() {
        return dataLoader;
    }

}