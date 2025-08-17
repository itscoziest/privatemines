package com.privatemines.listeners;

import com.privatemines.PrivateMines;
import com.privatemines.handlers.MineAccessHandler;
import com.privatemines.models.MineRegion;
import com.privatemines.utils.DebugUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.UUID;

public class BlockPlaceListener implements Listener {

    private final PrivateMines plugin;
    private final MineAccessHandler mineAccessHandler;

    public BlockPlaceListener(PrivateMines plugin, MineAccessHandler mineAccessHandler) {
        this.plugin = plugin;
        this.mineAccessHandler = mineAccessHandler;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Location location = event.getBlock().getLocation();

        // Only handle mines world
        if (!location.getWorld().getName().equals(plugin.getConfigManager().getWorldName())) {
            return;
        }

        // Find mine owner
        UUID mineOwner = plugin.getMineManager().getMineOwner(location);
        MineRegion region = mineOwner != null ? plugin.getMineManager().getMineRegion(mineOwner) : null;

        DebugUtils.debug(player, "Place check - Owner: " + (mineOwner != null) + ", Region: " + (region != null));

        if (region != null) {
            boolean inPlot = region.isInPlotArea(location);
            DebugUtils.debug(player, "In plot area: " + inPlot);
        }

        // Not in any mine region
        if (region == null) {
            event.setCancelled(true);
            player.sendMessage("§cYou cannot place blocks here!");
            return;
        }

        // Check ownership
        if (!mineOwner.equals(player.getUniqueId()) && !player.hasPermission("privatemines.bypass")) {
            event.setCancelled(true);
            player.sendMessage("§cThis is not your mine!");
            return;
        }

        // Only allow placing in plot area
        if (region.isInPlotArea(location)) {
            // Allow placement in plot area
        } else {
            event.setCancelled(true);
            player.sendMessage("§cYou can only place blocks in your plot area!");
        }
    }
}