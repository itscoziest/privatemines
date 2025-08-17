package com.privatemines.listeners;

import com.privatemines.PrivateMines;
import com.privatemines.handlers.AutoSellHandler;
import com.privatemines.handlers.MineAccessHandler;
import com.privatemines.models.MineData;
import com.privatemines.models.MineRegion;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.Map;
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

        // Find mine owner
        UUID mineOwner = plugin.getMineManager().getMineOwner(location);
        MineRegion region = mineOwner != null ? plugin.getMineManager().getMineRegion(mineOwner) : null;

        // Not in any mine - cancel
        if (region == null) {
            event.setCancelled(true);
            player.sendMessage("§cYou cannot break blocks here!");
            return;
        }

        // Check access
        boolean hasAccess = mineOwner.equals(player.getUniqueId()) ||
                (plugin.getVisitorSystem().isVisiting(player.getUniqueId()) &&
                        plugin.getVisitorSystem().getVisitingMine(player.getUniqueId()).equals(mineOwner)) ||
                player.hasPermission("privatemines.bypass");

        if (!hasAccess) {
            event.setCancelled(true);
            player.sendMessage("§cThis is not your mine!");
            return;
        }

        // Check what area we're in
        if (region.isInMiningArea(location)) {
            handleMiningAreaBreak(event, player, mineOwner);
        } else if (region.isInPlotArea(location)) {
            handlePlotAreaBreak(event, player, mineOwner);
        } else {
            event.setCancelled(true);
            player.sendMessage("§cYou cannot break blocks in this area!");
        }
    }

    /**
     * Handle mining area - ONLY allow mine blocks, replace with correct drops
     */
    private void handleMiningAreaBreak(BlockBreakEvent event, Player player, UUID mineOwner) {
        Material blockType = event.getBlock().getType();

        // Get mine configuration
        MineData mineData = plugin.getMineManager().getMineData(mineOwner);
        if (mineData == null) {
            event.setCancelled(true);
            return;
        }

        Map<String, Object> blockConfig = plugin.getConfigManager().getBlockConfig(mineData.getBlockIdentifier());

        // Check if this block type is allowed in the mine
        if (!blockConfig.containsKey(blockType.name())) {
            // This is NOT a mine block (structure block, etc.) - prevent breaking
            event.setCancelled(true);
            player.sendMessage("§cYou cannot break structure blocks!");
            return;
        }

        // This IS a mine block - allow breaking
        // Let the event proceed normally so enchants work with correct block

        // Track for auto-reset
        plugin.getMineManager().trackBlockRemoval(player, event.getBlock().getLocation());
    }

    /**
     * Handle plot area - only owner can build/break
     */
    private void handlePlotAreaBreak(BlockBreakEvent event, Player player, UUID mineOwner) {
        // Never allow grass breaking
        if (event.getBlock().getType() == Material.GRASS_BLOCK) {
            event.setCancelled(true);
            player.sendMessage("§cYou cannot break grass blocks!");
            return;
        }

        // Only mine owner can break in plot area
        if (!mineOwner.equals(player.getUniqueId()) && !player.hasPermission("privatemines.bypass")) {
            event.setCancelled(true);
            player.sendMessage("§cOnly the mine owner can build in the plot area!");
            return;
        }

        // Allow breaking for mine owner
    }
}