package com.privatemines.tasks;

import com.privatemines.PrivateMines;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class MineResetTask extends BukkitRunnable {

    private final PrivateMines plugin;
    private final UUID playerUuid;

    public MineResetTask(PrivateMines plugin, UUID playerUuid) {
        this.plugin = plugin;
        this.playerUuid = playerUuid;
    }

    @Override
    public void run() {
        plugin.getMineManager().resetMine(playerUuid);
    }
}