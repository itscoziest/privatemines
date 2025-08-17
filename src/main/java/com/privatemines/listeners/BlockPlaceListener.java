package com.privatemines.listeners;

import com.privatemines.PrivateMines;
import com.privatemines.handlers.MineAccessHandler;
import com.privatemines.models.MineRegion;
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

        // Debug output
        player.sendMessage("§eDEBUG: Placing - MineOwner=" + (mineOwner != null) + " Region=" + (region != null));

        if (region != null) {
            boolean inPlot = region.isInPlotArea(location);
            player.sendMessage("§eDEBUG: InPlot=" + inPlot);
        }

        // Not in any mine region
        if (region == null) {
            event.setCancelled(true);
            player.sendMessage("§cNot in any mine region!");
            return;
        }

        // Check ownership
        if (!mineOwner.equals(player.getUniqueId()) && !player.hasPermission("privatemines.bypass")) {
            event.setCancelled(true);
            player.sendMessage("§cNot your mine!");
            return;
        }

        // Only allow placing in plot area
        if (region.isInPlotArea(location)) {
            player.sendMessage("§aPlot area - placement allowed");
        } else {
            event.setCancelled(true);
            player.sendMessage("§cCan only place in plot area above grass blocks!");
        }
    }
}