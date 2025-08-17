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
import java.util.Map;
import java.util.UUID;


public class PrivateMines extends JavaPlugin {

    private static PrivateMines instance;

    private ConfigManager configManager;
    private DataManager dataManager;
    private MineManager mineManager;
    private SchematicManager schematicManager;
    private PoolManager poolManager;
    private PrivateMinesAPI api;
    private WorldGuardManager worldGuardManager;
    private AutoSellHandler autoSellHandler;
    private MineAccessHandler mineAccessHandler;
    private com.privatemines.utils.VisitorSystem visitorSystem;

    @Override
    public void onEnable() {
        instance = this;

        // Initialize debug utils first
        com.privatemines.utils.DebugUtils.init(this);

        initializeManagers();
        registerCommands();
        registerListeners();
        startTasks();

        getLogger().info("PrivateMines enabled successfully!");

        if (configManager.isDebugEnabled()) {
            getLogger().info("Debug mode is ENABLED");
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("PrivateMines shutting down...");

        try {
            // Cancel all active tasks first
            Bukkit.getScheduler().cancelTasks(this);

            // Save data SYNCHRONOUSLY during shutdown (no async tasks)
            if (dataManager != null) {
                getLogger().info("Saving all mine data synchronously...");
                long startTime = System.currentTimeMillis();

                // Get all mine data
                Map<UUID, com.privatemines.models.MineData> allMines = dataManager.getAllMines();

                // Save each mine synchronously
                for (Map.Entry<UUID, com.privatemines.models.MineData> entry : allMines.entrySet()) {
                    try {
                        dataManager.saveMineDataSync(entry.getKey(), entry.getValue());
                    } catch (Exception e) {
                        getLogger().warning("Failed to save mine for " + entry.getKey() + ": " + e.getMessage());
                    }
                }

                long duration = System.currentTimeMillis() - startTime;
                getLogger().info("Saved " + allMines.size() + " mines in " + duration + "ms");
            }

        } catch (Exception e) {
            getLogger().severe("Error during shutdown: " + e.getMessage());
        }

        getLogger().info("PrivateMines disabled successfully!");
    }

    private void initializeManagers() {
        configManager = new ConfigManager(this);
        dataManager = new DataManager(this);
        poolManager = new PoolManager(this);
        schematicManager = new SchematicManager(this);
        mineManager = new MineManager(this);
        worldGuardManager = new WorldGuardManager(this);

        // Initialize handlers and systems
        mineAccessHandler = new MineAccessHandler(this);
        autoSellHandler = new AutoSellHandler(this);
        visitorSystem = new com.privatemines.utils.VisitorSystem(this);

        api = new PrivateMinesAPI(mineManager);

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new com.privatemines.placeholders.PrivateMinesExpansion(this).register();
        }
    }

    private void registerCommands() {
        getCommand("pmine").setExecutor(new MainCommand(this));
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerQuitListener(this), this);
        Bukkit.getPluginManager().registerEvents(new BlockBreakListener(this, mineAccessHandler, autoSellHandler), this);
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

    public AutoSellHandler getAutoSellHandler() {
        return autoSellHandler;
    }

    public MineAccessHandler getMineAccessHandler() {
        return mineAccessHandler;
    }

    public com.privatemines.utils.VisitorSystem getVisitorSystem() {
        return visitorSystem;
    }

    public PrivateMinesAPI getAPI() {
        return api;
    }
}