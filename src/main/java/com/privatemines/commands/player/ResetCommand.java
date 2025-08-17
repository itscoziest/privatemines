package com.privatemines.commands.player;

import com.privatemines.PrivateMines;
import com.privatemines.models.MineData;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ResetCommand {

    private final PrivateMines plugin;

    public ResetCommand(PrivateMines plugin) {
        this.plugin = plugin;
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
            player.sendMessage(plugin.getConfigManager().getMessage("no_mine"));
            return true;
        }

        player.sendMessage("§6Resetting your mine...");

        plugin.getMineManager().resetMine(player.getUniqueId());

        return true;
    }
}