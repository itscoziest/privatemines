package com.privatemines.managers;

import com.privatemines.PrivateMines;
import com.privatemines.models.MineData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DataManager {

    private final PrivateMines plugin;
    private final Map<UUID, MineData> mineDataCache;
    private File dataFile;
    private FileConfiguration data;

    public DataManager(PrivateMines plugin) {
        this.plugin = plugin;
        this.mineDataCache = new HashMap<>();
        loadData();
    }

    public void loadData() {
        dataFile = new File(plugin.getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            plugin.saveResource("data.yml", false);
        }

        data = YamlConfiguration.loadConfiguration(dataFile);
        loadAllMines();
    }

    private void loadAllMines() {
        if (!data.contains("mines")) return;

        for (String uuidStr : data.getConfigurationSection("mines").getKeys(false)) {
            UUID uuid = UUID.fromString(uuidStr);
            String path = "mines." + uuidStr;

            int level = data.getInt(path + ".level");
            String blockId = data.getString(path + ".block_identifier");
            String owner = data.getString(path + ".owner");

            String locStr = data.getString(path + ".location");
            String[] parts = locStr.split(",");
            Location location = new Location(
                    Bukkit.getWorld(parts[0].trim()),
                    Double.parseDouble(parts[1].trim()),
                    Double.parseDouble(parts[2].trim()),
                    Double.parseDouble(parts[3].trim())
            );

            MineData mineData = new MineData(uuid, level, blockId, location, owner);
            mineDataCache.put(uuid, mineData);
        }
    }

    /**
     * Save mine data synchronously (for shutdown and high-performance saves)
     */
    public void saveMineDataSync(UUID playerUuid, MineData mineData) {
        try {
            // Use your existing data storage method
            // Just save directly without async operations
            setMineData(playerUuid, mineData);

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save mine data synchronously: " + e.getMessage());
        }
    }

    /**
     * Optimized saveAllData - only async during normal operation
     */
    public void saveAllData() {
        if (!plugin.isEnabled()) {
            // Plugin shutting down - call synchronous save instead
            saveAllDataSync();
            return;
        }

        // Only use async during normal operation
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                saveAllDataSync();
                plugin.getLogger().info("Auto-saved all mine data");
            } catch (Exception e) {
                plugin.getLogger().severe("Error during auto-save: " + e.getMessage());
            }
        });
    }

    /**
     * Synchronous save method for shutdown and critical saves
     */
    public void saveAllDataSync() {
        try {
            Map<UUID, MineData> allMines = getAllMines();
            for (Map.Entry<UUID, MineData> entry : allMines.entrySet()) {
                saveMineDataSync(entry.getKey(), entry.getValue());
            }
            // Force save the file
            saveDataFile();
            plugin.getLogger().info("Synchronously saved " + allMines.size() + " mines");
        } catch (Exception e) {
            plugin.getLogger().severe("Error during synchronous save: " + e.getMessage());
        }
    }

    public void saveMineData(MineData mineData) {
        String path = "mines." + mineData.getUuid().toString();

        data.set(path + ".level", mineData.getLevel());
        data.set(path + ".block_identifier", mineData.getBlockIdentifier());
        data.set(path + ".owner", mineData.getOwner());

        Location loc = mineData.getLocation();
        String locStr = loc.getWorld().getName() + ", " + loc.getX() + ", " + loc.getY() + ", " + loc.getZ();
        data.set(path + ".location", locStr);

        // CRITICAL FIX: Actually save the file to disk!
        saveDataFile();
    }

    public void saveDataFile() {
        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save data.yml: " + e.getMessage());
        }
    }

    public void deleteMineData(UUID uuid) {
        mineDataCache.remove(uuid);
        data.set("mines." + uuid.toString(), null);
        saveDataFile();
    }

    public MineData getMineData(UUID uuid) {
        return mineDataCache.get(uuid);
    }

    public void setMineData(UUID uuid, MineData mineData) {
        mineDataCache.put(uuid, mineData);
        saveMineData(mineData);
    }

    public boolean hasMine(UUID uuid) {
        return mineDataCache.containsKey(uuid);
    }

    public Map<UUID, MineData> getAllMines() {
        return new HashMap<>(mineDataCache);
    }

    // Add method to expose data configuration for location saving
    public org.bukkit.configuration.file.FileConfiguration getData() {
        return data;
    }
}