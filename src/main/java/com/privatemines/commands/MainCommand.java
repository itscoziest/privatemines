package com.privatemines.commands;

import com.privatemines.PrivateMines;
import com.privatemines.commands.admin.*;
import com.privatemines.commands.player.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MainCommand implements CommandExecutor {

    private final PrivateMines plugin;
    private final TeleportCommand teleportCommand;
    private final ResetCommand resetCommand;
    private final AdminResetCommand adminResetCommand;
    private final SetLevelCommand setLevelCommand;
    private final SetBlockCommand setBlockCommand;
    private final DeleteCommand deleteCommand;

    public MainCommand(PrivateMines plugin) {
        this.plugin = plugin;
        this.teleportCommand = new TeleportCommand(plugin);
        this.resetCommand = new ResetCommand(plugin);
        this.adminResetCommand = new AdminResetCommand(plugin);
        this.setLevelCommand = new SetLevelCommand(plugin);
        this.setBlockCommand = new SetBlockCommand(plugin);
        this.deleteCommand = new DeleteCommand(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        sender.sendMessage("§aDEBUG: PrivateMines command received!"); // DEBUG LINE
        if (args.length == 0 || args[0].equalsIgnoreCase("tp")) {
            return teleportCommand.execute(sender, args);
        }

        switch (args[0].toLowerCase()) {
            case "reset":
                return resetCommand.execute(sender, args);

            case "admin":
                if (!sender.hasPermission("privatemines.admin")) {
                    sender.sendMessage(plugin.getConfigManager().getMessage("no_permission"));
                    return true;
                }
                return handleAdminCommand(sender, args);

            default:
                sendHelpMessage(sender);
                return true;
        }
    }

    private boolean handleAdminCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendAdminHelp(sender);
            return true;
        }

        switch (args[1].toLowerCase()) {
            case "reset":
                return adminResetCommand.execute(sender, args);
            case "setlevel":
                return setLevelCommand.execute(sender, args);
            case "setblock":
                return setBlockCommand.execute(sender, args);
            case "delete":
                return deleteCommand.execute(sender, args);
            default:
                sendAdminHelp(sender);
                return true;
        }
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage("§6§lPrivateMines Commands:");
        sender.sendMessage("§e/pmine §7- Teleport to your mine");
        sender.sendMessage("§e/pmine reset §7- Reset your mine");

        if (sender.hasPermission("privatemines.admin")) {
            sender.sendMessage("§e/pmine admin §7- Admin commands");
        }
    }

    private void sendAdminHelp(CommandSender sender) {
        sender.sendMessage("§6§lPrivateMines Admin Commands:");
        sender.sendMessage("§e/pmine admin reset <player> §7- Reset player's mine");
        sender.sendMessage("§e/pmine admin setlevel <player> <level> §7- Set mine level");
        sender.sendMessage("§e/pmine admin setblock <player> <identifier> §7- Set mine blocks");
        sender.sendMessage("§e/pmine admin delete <player> §7- Delete player's mine");
    }
}