// VisitCommand.java
package com.privatemines.commands.player;

import com.privatemines.PrivateMines;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;

public class VisitCommand {

    private final PrivateMines plugin;

    public VisitCommand(PrivateMines plugin) {
        this.plugin = plugin;
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("privatemines.visit")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no_permission"));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(plugin.getConfigManager().getMessage("visit_usage"));
            return true;
        }

        String targetPlayer = args[1];

        // Add 3 second delay
        player.sendMessage("§aTeleporting to " + targetPlayer + "'s mine in 3 seconds...");

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            boolean success = plugin.getVisitorSystem().visitMine(player, targetPlayer);

            if (success) {
                player.sendMessage(plugin.getConfigManager().getMessage("visit_success")
                        .replace("{player}", targetPlayer));
            } else {
                player.sendMessage(plugin.getConfigManager().getMessage("visit_failed")
                        .replace("{player}", targetPlayer));
            }
        }, 60L); // 3 seconds = 60 ticks

        return true;
    }
}