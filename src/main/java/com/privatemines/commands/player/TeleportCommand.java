package com.privatemines.commands.player;

import com.privatemines.PrivateMines;
import com.privatemines.models.MineData;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TeleportCommand {

    private final PrivateMines plugin;

    public TeleportCommand(PrivateMines plugin) {
        this.plugin = plugin;
    }

    public boolean execute(CommandSender sender, String[] args) {
        sender.sendMessage("§eDEBUG: TeleportCommand.execute() called");

        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        Player player = (Player) sender;
        sender.sendMessage("§eDEBUG: Player check passed");

        if (!player.hasPermission("privatemines.use")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no_permission"));
            sender.sendMessage("§eDEBUG: Permission denied");
            return true;
        }

        sender.sendMessage("§eDEBUG: Permission check passed");

        MineData mineData = plugin.getMineManager().getMineData(player.getUniqueId());
        sender.sendMessage("§eDEBUG: Checked for existing mine: " + (mineData != null));

        if (mineData == null) {
            sender.sendMessage("§eDEBUG: Creating new mine...");

            plugin.getMineManager().createMine(player).thenAccept(success -> {
                sender.sendMessage("§eDEBUG: Mine creation result: " + success);
                if (success) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        teleportPlayer(player);
                    });
                } else {
                    player.sendMessage("§cFailed to create your mine!");
                }
            }).exceptionally(throwable -> {
                sender.sendMessage("§cDEBUG: Mine creation error: " + throwable.getMessage());
                throwable.printStackTrace();
                return null;
            });
            return true;
        }

        sender.sendMessage("§eDEBUG: Mine exists, teleporting...");
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
            player.teleport(mineData.getLocation());
            player.sendMessage(plugin.getConfigManager().getMessage("teleported"));
        }
    }
}