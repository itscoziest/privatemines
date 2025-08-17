package com.privatemines.commands.admin;

import com.privatemines.PrivateMines;
import com.privatemines.models.MineData;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DeleteCommand {

    private final PrivateMines plugin;
    private final Map<String, Long> confirmations;

    public DeleteCommand(PrivateMines plugin) {
        this.plugin = plugin;
        this.confirmations = new HashMap<>();
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /pmine admin delete <player> [confirm]");
            return true;
        }

        String targetName = args[2];
        boolean isConfirming = args.length > 3 && args[3].equalsIgnoreCase("confirm");

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

        String confirmKey = sender.getName() + ":" + targetName;

        if (!isConfirming) {
            confirmations.put(confirmKey, System.currentTimeMillis());
            String message = plugin.getConfigManager().getMessage("confirm_delete").replace("{player}", targetName);
            sender.sendMessage(message);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                confirmations.remove(confirmKey);
            }, 600L);

            return true;
        }

        if (!confirmations.containsKey(confirmKey)) {
            sender.sendMessage("§cConfirmation expired! Please run the command again.");
            return true;
        }

        long confirmTime = confirmations.get(confirmKey);
        if (System.currentTimeMillis() - confirmTime > 30000) {
            confirmations.remove(confirmKey);
            sender.sendMessage("§cConfirmation expired! Please run the command again.");
            return true;
        }

        confirmations.remove(confirmKey);
        plugin.getMineManager().deleteMine(targetUuid);

        sender.sendMessage("§aSuccessfully deleted " + targetName + "'s mine!");

        if (targetPlayer != null && targetPlayer.isOnline()) {
            targetPlayer.sendMessage("§cYour mine has been deleted by an administrator!");
        }

        return true;
    }
}