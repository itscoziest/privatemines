package com.privatemines.tasks;

import com.privatemines.managers.DataManager;
import org.bukkit.scheduler.BukkitRunnable;

public class AutoSaveTask extends BukkitRunnable {

    private final DataManager dataManager;

    public AutoSaveTask(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    @Override
    public void run() {
        dataManager.saveAllData();
    }
}