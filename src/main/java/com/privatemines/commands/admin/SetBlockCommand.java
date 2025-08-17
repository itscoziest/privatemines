package com.privatemines.commands.admin;

import com.privatemines.PrivateMines;
import com.privatemines.models.MineData;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class SetBlockCommand {

    private final PrivateMines plugin;

    public SetBlockCommand(PrivateMines plugin) {
        this.plugin = plugin;
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cUsage: /pmine admin setblock <player> <identifier>");
            return true;
        }

        String targetName = args[2];
        String blockIdentifier = args[3];

        if (!plugin.getConfigManager().hasBlockConfig(blockIdentifier)) {
            sender.sendMessage("§cBlock configuration '" + blockIdentifier + "' not found!");
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

        plugin.getMineManager().setMineBlocks(targetUuid, blockIdentifier);
        sender.sendMessage("§aSet " + targetName + "'s mine blocks to '" + blockIdentifier + "'");

        if (targetPlayer != null && targetPlayer.isOnline()) {
            targetPlayer.sendMessage("§aYour mine blocks have been updated to '" + blockIdentifier + "'!");
        }

        return true;
    }
}