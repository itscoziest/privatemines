package com.privatemines.commands.player;

import com.privatemines.PrivateMines;
import com.privatemines.models.MineData;
import com.privatemines.utils.MineLoadingSystem;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Location;

public class TeleportCommand {

    private final PrivateMines plugin;
    private final MineLoadingSystem loadingSystem;

    public TeleportCommand(PrivateMines plugin) {
        this.plugin = plugin;
        this.loadingSystem = new MineLoadingSystem(plugin);
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("privatemines.use")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no_permission"));
            return true;
        }

        MineData mineData = plugin.getMineManager().getMineData(player.getUniqueId());

        if (mineData == null) {
            // Start loading experience for mine creation
            loadingSystem.startLoading(player);

            // Create new mine
            plugin.getMineManager().createMine(player).thenAccept(success -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    // Stop loading experience
                    loadingSystem.stopLoading(player);

                    if (success) {
                        teleportPlayer(player);
                    } else {
                        player.sendMessage("§cFailed to create your mine!");
                    }
                });
            }).exceptionally(throwable -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    // Stop loading experience on error
                    loadingSystem.stopLoading(player);
                    player.sendMessage("§cAn error occurred while creating your mine!");
                });
                plugin.getLogger().severe("Mine creation error for " + player.getName() + ": " + throwable.getMessage());
                throwable.printStackTrace();
                return null;
            });
            return true;
        }

        // Mine exists, teleport to it
        teleportPlayer(player);
        return true;
    }

    private void teleportPlayer(Player player) {
        int delay = plugin.getConfigManager().getTeleportDelay();

        if (delay > 0) {
            String message = plugin.getConfigManager().getMessage("teleporting").replace("{time}", String.valueOf(delay));
            player.sendMessage(message);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                performTeleport(player);
            }, delay * 20L);
        } else {
            performTeleport(player);
        }
    }

    private void performTeleport(Player player) {
        MineData mineData = plugin.getMineManager().getMineData(player.getUniqueId());
        if (mineData != null) {
            Location teleportLoc = mineData.getLocation();

            // Fix: Ensure world is loaded
            if (teleportLoc.getWorld() == null) {
                // Try to get the world by name
                String worldName = plugin.getConfigManager().getWorldName();
                org.bukkit.World world = plugin.getServer().getWorld(worldName);

                if (world == null) {
                    player.sendMessage("§cMines world not loaded! Please try again.");
                    return;
                }

                // Create new location with proper world
                teleportLoc = new Location(world, teleportLoc.getX(), teleportLoc.getY(), teleportLoc.getZ());
            }

            player.teleport(teleportLoc);
            player.sendMessage(plugin.getConfigManager().getMessage("teleported"));
        } else {
            player.sendMessage("§cCould not find your mine data!");
        }
    }
}