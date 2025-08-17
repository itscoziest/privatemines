package com.privatemines.commands.admin;

import com.privatemines.PrivateMines;
import com.privatemines.models.MineData;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class AdminResetCommand {

    private final PrivateMines plugin;

    public AdminResetCommand(PrivateMines plugin) {
        this.plugin = plugin;
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /pmine admin reset <player>");
            return true;
        }

        String targetName = args[2];
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

        sender.sendMessage("§6Resetting " + targetName + "'s mine...");

        UUID finalTargetUuid = targetUuid;
        plugin.getMineManager().resetMine(targetUuid).thenRun(() -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage("§aSuccessfully reset " + targetName + "'s mine!");

                if (targetPlayer != null && targetPlayer.isOnline()) {
                    targetPlayer.sendMessage(plugin.getConfigManager().getMessage("mine_reset"));
                }
            });
        });

        return true;
    }
}