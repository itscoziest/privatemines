package com.privatemines.commands.player;

import com.privatemines.PrivateMines;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FlyCommand implements CommandExecutor {

    private final PrivateMines plugin;

    public FlyCommand(PrivateMines plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Only players can use this command
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        // Check permission
        if (!player.hasPermission("privatemines.fly")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
            return true;
        }

        // Check if player is in private mines world
        if (!player.getWorld().getName().equals(plugin.getConfigManager().getWorldName())) {
            player.sendMessage(ChatColor.RED + "You can only use fly in the private mines world!");
            return true;
        }

        // Toggle fly mode
        if (player.getAllowFlight()) {
            player.setAllowFlight(false);
            player.setFlying(false);
            player.sendMessage(ChatColor.RED + "Fly mode disabled!");
        } else {
            player.setAllowFlight(true);
            player.setFlying(true);
            player.sendMessage(ChatColor.GREEN + "Fly mode enabled!");
        }

        return true;
    }
}