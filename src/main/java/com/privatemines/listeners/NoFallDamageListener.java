package com.privatemines.listeners;

import com.privatemines.PrivateMines;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class NoFallDamageListener implements Listener {

    private final PrivateMines plugin;

    public NoFallDamageListener(PrivateMines plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        // Only handle players
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();

        // Only in private mines world
        if (!player.getWorld().getName().equals(plugin.getConfigManager().getWorldName())) {
            return;
        }

        // Cancel fall damage in private mines world
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            event.setCancelled(true);
        }
    }
}