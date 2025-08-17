package com.privatemines.commands.player;

import com.privatemines.PrivateMines;
import com.privatemines.models.MineData;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ToggleVisitorCommand {

    private final PrivateMines plugin;

    public ToggleVisitorCommand(PrivateMines plugin) {
        this.plugin = plugin;
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("privatemines.toggle")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no_permission"));
            return true;
        }

        // Check if player has a mine
        MineData mineData = plugin.getMineManager().getMineData(player.getUniqueId());
        if (mineData == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("no_mine"));
            return true;
        }

        boolean newSetting = plugin.getVisitorSystem().toggleVisitors(player.getUniqueId());

        String message = newSetting ?
                plugin.getConfigManager().getMessage("visitors_enabled") :
                plugin.getConfigManager().getMessage("visitors_disabled");

        player.sendMessage(message);

        return true;
    }
}