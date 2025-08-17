package com.privatemines.listeners;

import com.privatemines.PrivateMines;
import org.bukkit.entity.Player;
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
        Player player = event.getPlayer();

        // Clean up visitor data when player leaves
        plugin.getVisitorSystem().onPlayerQuit(player.getUniqueId());
    }
}