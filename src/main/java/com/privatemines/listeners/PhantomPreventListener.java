package com.privatemines.listeners;

import com.privatemines.PrivateMines;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Phantom;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntitySpawnEvent;

public class PhantomPreventListener implements Listener {

    private final PrivateMines plugin;

    public PhantomPreventListener(PrivateMines plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPhantomSpawn(CreatureSpawnEvent event) {
        // Check if it's a phantom
        if (event.getEntityType() != EntityType.PHANTOM) {
            return;
        }

        // Only in private mines world
        if (!event.getLocation().getWorld().getName().equals(plugin.getConfigManager().getWorldName())) {
            return;
        }

        // Cancel phantom spawning in private mines world
        event.setCancelled(true);
    }
}