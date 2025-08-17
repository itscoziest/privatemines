package com.privatemines.listeners;

import com.privatemines.PrivateMines;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class PlayerJoinListener implements Listener {

    private final PrivateMines plugin;

    public PlayerJoinListener(PrivateMines plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Send mine blocks to player if they have a mine
    }
}