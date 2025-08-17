package com.privatemines.managers;

import com.privatemines.PrivateMines;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class ConfigManager {

    private final PrivateMines plugin;
    private FileConfiguration config;
    private FileConfiguration blocksConfig;
    private File configFile;
    private File blocksFile;

    public ConfigManager(PrivateMines plugin) {
        this.plugin = plugin;
        loadConfigs();
    }

    public void loadConfigs() {
        plugin.saveDefaultConfig();
        config = plugin.getConfig();

        createBlocksConfig();
        loadBlocksConfig();
    }

    private void createBlocksConfig() {
        blocksFile = new File(plugin.getDataFolder(), "blocks.yml");
        if (!blocksFile.exists()) {
            plugin.saveResource("blocks.yml", false);
        }
    }

    private void loadBlocksConfig() {
        if (blocksFile == null) {
            createBlocksConfig();
        }
        blocksConfig = YamlConfiguration.loadConfiguration(blocksFile);
    }

    public void reloadConfigs() {
        plugin.reloadConfig();
        config = plugin.getConfig();
        loadBlocksConfig();
    }

    public void saveBlocksConfig() {
        try {
            blocksConfig.save(blocksFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save blocks.yml: " + e.getMessage());
        }
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public FileConfiguration getBlocksConfig() {
        return blocksConfig;
    }

    public String getWorldName() {
        return config.getString("world.name", "mines_world");
    }

    public int getWorldSpacing() {
        return config.getInt("world.spacing", 50);
    }

    public int getStartX() {
        return config.getInt("world.start_x", 0);
    }

    public int getStartZ() {
        return config.getInt("world.start_z", 0);
    }

    public int getDefaultLevel() {
        return config.getInt("mine.default_level", 1);
    }

    public int getMaxLevel() {
        return config.getInt("mine.max_level", 10);
    }

    public String getDefaultBlocks() {
        return config.getString("mine.default_blocks", "mine-1");
    }

    public int getPlotHeight() {
        return config.getInt("mine.plot_height", 50);
    }

    public int getTeleportDelay() {
        return config.getInt("mine.teleport_delay", 3);
    }

    public String getSchematicFolder() {
        return config.getString("schematic.folder", "schematics");
    }

    public String getDefaultSchematic() {
        return config.getString("schematic.default_file", "default_mine.schem");
    }

    public int getMineSize(int level) {
        return config.getInt("sizes." + level, 50);
    }

    public String getMessage(String key) {
        return config.getString("messages." + key, "&cMessage not found: " + key);
    }

    public Map<String, Object> getBlockConfig(String identifier) {
        return blocksConfig.getConfigurationSection(identifier).getValues(false);
    }

    public boolean hasBlockConfig(String identifier) {
        return blocksConfig.contains(identifier);
    }
}