package com.privatemines.listeners;

import com.privatemines.PrivateMines;
import com.privatemines.models.MineData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

    private final PrivateMines plugin;

    public PlayerQuitListener(PrivateMines plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        MineData mineData = plugin.getDataManager().getMineData(event.getPlayer().getUniqueId());
        if (mineData != null) {
            plugin.getDataManager().saveMineData(mineData);
        }
    }
}