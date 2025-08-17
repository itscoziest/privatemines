package com.privatemines;

import com.privatemines.api.PrivateMinesAPI;
import com.privatemines.commands.MainCommand;
import com.privatemines.listeners.*;
import com.privatemines.managers.*;
import com.privatemines.tasks.AutoSaveTask;
import org.bukkit.Bukkit;
import com.privatemines.handlers.AutoSellHandler;
import com.privatemines.handlers.MineAccessHandler;
import org.bukkit.plugin.java.JavaPlugin;

public class PrivateMines extends JavaPlugin {

    private static PrivateMines instance;

    private ConfigManager configManager;
    private DataManager dataManager;
    private MineManager mineManager;
    private SchematicManager schematicManager;
    private PoolManager poolManager;
    private PrivateMinesAPI api;
    private WorldGuardManager worldGuardManager;

    @Override
    public void onEnable() {
        instance = this;

        initializeManagers();
        registerCommands();
        registerListeners();
        startTasks();

        getLogger().info("PrivateMines enabled successfully!");
    }

    @Override
    public void onDisable() {
        if (dataManager != null) {
            dataManager.saveAllData();
        }
        getLogger().info("PrivateMines disabled!");
    }

    private void initializeManagers() {
        configManager = new ConfigManager(this);
        dataManager = new DataManager(this);
        poolManager = new PoolManager(this);
        schematicManager = new SchematicManager(this);
        mineManager = new MineManager(this);
        worldGuardManager = new WorldGuardManager(this);
        api = new PrivateMinesAPI(mineManager);

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new com.privatemines.placeholders.PrivateMinesExpansion(this).register();
        }
    }

    private void registerCommands() {
        getCommand("pmine").setExecutor(new MainCommand(this));
    }

    private void registerListeners() {
        MineAccessHandler mineAccessHandler = new MineAccessHandler(this);
        AutoSellHandler autoSellHandler = new AutoSellHandler(this);

        Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerQuitListener(this), this);
        Bukkit.getPluginManager().registerEvents(new BlockPlaceListener(this, mineAccessHandler), this);
        Bukkit.getPluginManager().registerEvents(new PlayerMoveListener(this), this);
    }

    private void startTasks() {
        if (configManager.getConfig().getBoolean("auto_save.enabled")) {
            int interval = configManager.getConfig().getInt("auto_save.interval") * 20;
            new AutoSaveTask(dataManager).runTaskTimer(this, interval, interval);
        }
    }

    public static PrivateMines getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public MineManager getMineManager() {
        return mineManager;
    }

    public SchematicManager getSchematicManager() {
        return schematicManager;
    }

    public PoolManager getPoolManager() {
        return poolManager;
    }

    public WorldGuardManager getWorldGuardManager() {
        return worldGuardManager;
    }

    public PrivateMinesAPI getAPI() {
        return api;
    }
}