package com.privatemines.listeners;

import com.privatemines.PrivateMines;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;

public class WorldChangeListener implements Listener {

    private final PrivateMines plugin;

    public WorldChangeListener(PrivateMines plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        String minesWorld = plugin.getConfigManager().getWorldName();

        // If player left the mines world, disable fly
        if (event.getFrom().getName().equals(minesWorld) &&
                !player.getWorld().getName().equals(minesWorld)) {

            // Only disable if they don't have EssentialsX fly active
            // (by checking if they have the essentials.fly permission and it's not from PrivateMines)
            if (player.getAllowFlight() && !player.hasPermission("essentials.fly")) {
                player.setAllowFlight(false);
                player.setFlying(false);
                player.sendMessage(ChatColor.YELLOW + "Fly mode disabled (left private mines world)");
            }
        }
    }
}