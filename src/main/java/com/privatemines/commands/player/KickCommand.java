package com.privatemines.commands.player;

import com.privatemines.PrivateMines;
import com.privatemines.models.MineData;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;

public class KickCommand {

    private final PrivateMines plugin;

    public KickCommand(PrivateMines plugin) {
        this.plugin = plugin;
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("privatemines.kick")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no_permission"));
            return true;
        }

        // Check if player has a mine
        MineData mineData = plugin.getMineManager().getMineData(player.getUniqueId());
        if (mineData == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("no_mine"));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage("§cUsage: /pmine kick <player/all>");
            return true;
        }

        String target = args[1];

        if (target.equalsIgnoreCase("all")) {
            // Kick all visitors
            int kicked = plugin.getVisitorSystem().kickAllVisitors(player.getUniqueId());
            player.sendMessage("§aKicked " + kicked + " visitors from your mine!");
        } else {
            // Kick specific player
            Player targetPlayer = Bukkit.getPlayer(target);
            if (targetPlayer == null) {
                player.sendMessage("§cPlayer not found!");
                return true;
            }

            boolean success = plugin.getVisitorSystem().kickVisitor(player.getUniqueId(), targetPlayer.getUniqueId());
            if (success) {
                player.sendMessage("§aKicked " + target + " from your mine!");
                targetPlayer.sendMessage("§cYou were kicked from " + player.getName() + "'s mine!");
            } else {
                player.sendMessage("§c" + target + " is not visiting your mine!");
            }
        }

        return true;
    }
}