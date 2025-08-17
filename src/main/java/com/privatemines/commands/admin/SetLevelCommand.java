package com.privatemines.commands.admin;

import com.privatemines.PrivateMines;
import com.privatemines.models.MineData;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class SetLevelCommand {

    private final PrivateMines plugin;

    public SetLevelCommand(PrivateMines plugin) {
        this.plugin = plugin;
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cUsage: /pmine admin setlevel <player> <level>");
            return true;
        }

        String targetName = args[2];
        String levelStr = args[3];

        int level;
        try {
            level = Integer.parseInt(levelStr);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid level number!");
            return true;
        }

        int maxLevel = plugin.getConfigManager().getMaxLevel();
        if (level < 1 || level > maxLevel) {
            sender.sendMessage(plugin.getConfigManager().getMessage("invalid_level"));
            return true;
        }

        Player targetPlayer = Bukkit.getPlayer(targetName);
        UUID targetUuid = null;

        if (targetPlayer != null) {
            targetUuid = targetPlayer.getUniqueId();
        } else {
            for (MineData mineData : plugin.getDataManager().getAllMines().values()) {
                if (mineData.getOwner().equalsIgnoreCase(targetName)) {
                    targetUuid = mineData.getUuid();
                    break;
                }
            }
        }

        if (targetUuid == null) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player_not_found"));
            return true;
        }

        MineData mineData = plugin.getMineManager().getMineData(targetUuid);
        if (mineData == null) {
            sender.sendMessage("§cPlayer doesn't have a mine!");
            return true;
        }

        plugin.getMineManager().setMineLevel(targetUuid, level);
        sender.sendMessage("§aSet " + targetName + "'s mine level to " + level);

        if (targetPlayer != null && targetPlayer.isOnline()) {
            targetPlayer.sendMessage("§aYour mine level has been set to " + level + "!");
        }

        return true;
    }
}