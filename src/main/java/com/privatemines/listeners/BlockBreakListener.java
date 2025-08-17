package com.privatemines.listeners;

import com.privatemines.PrivateMines;
import com.privatemines.handlers.AutoSellHandler;
import com.privatemines.handlers.MineAccessHandler;
import com.privatemines.models.MineRegion;
import com.privatemines.utils.PacketUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.UUID;

public class BlockBreakListener implements Listener {

    private final PrivateMines plugin;
    private final MineAccessHandler mineAccessHandler;
    private final AutoSellHandler autoSellHandler;

    public BlockBreakListener(PrivateMines plugin, MineAccessHandler mineAccessHandler, AutoSellHandler autoSellHandler) {
        this.plugin = plugin;
        this.mineAccessHandler = mineAccessHandler;
        this.autoSellHandler = autoSellHandler;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Location location = event.getBlock().getLocation();

        // Only handle mines world
        if (!location.getWorld().getName().equals(plugin.getConfigManager().getWorldName())) {
            return;
        }

        // Find which mine this location belongs to
        UUID mineOwner = plugin.getMineManager().getMineOwner(location);
        MineRegion region = mineOwner != null ? plugin.getMineManager().getMineRegion(mineOwner) : null;

        // Debug output
        player.sendMessage("§eDEBUG: MineOwner=" + (mineOwner != null) + " Region=" + (region != null));

        if (region != null) {
            boolean inMining = region.isInMiningArea(location);
            boolean inPlot = region.isInPlotArea(location);
            player.sendMessage("§eDEBUG: InMining=" + inMining + " InPlot=" + inPlot);
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

        // Never allow grass breaking
        if (location.getBlock().getType() == Material.GRASS_BLOCK) {
            event.setCancelled(true);
            player.sendMessage("§cCannot break grass blocks!");
            return;
        }

        // Check areas
        if (region.isInPlotArea(location)) {
            player.sendMessage("§aPlot area - normal break allowed");
            return; // Allow normal breaking in plot area
        } else if (region.isInMiningArea(location)) {
            // Mining area - fake break
            event.setCancelled(true);
            player.sendMessage("§aMining area - fake break");
            autoSellHandler.handleBlockBreak(player, event.getBlock());
            PacketUtils.sendBlockChange(player, location, Material.AIR);
        } else {
            // Outside both areas
            event.setCancelled(true);
            player.sendMessage("§cNot in valid mining or plot area!");
        }
    }
}