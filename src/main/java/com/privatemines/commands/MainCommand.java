package com.privatemines.commands;

import com.privatemines.PrivateMines;
import com.privatemines.commands.admin.*;
import com.privatemines.commands.player.*;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import com.privatemines.commands.player.VisitCommand;
import com.privatemines.commands.player.ToggleVisitorCommand;
import org.bukkit.entity.Player;

public class MainCommand implements CommandExecutor {

    private final PrivateMines plugin;
    private final TeleportCommand teleportCommand;
    private final ResetCommand resetCommand;
    private final AdminResetCommand adminResetCommand;
    private final SetLevelCommand setLevelCommand;
    private final SetBlockCommand setBlockCommand;
    private final DeleteCommand deleteCommand;
    private final VisitCommand visitCommand;
    private final ToggleVisitorCommand toggleVisitorCommand;
    private final KickCommand kickCommand;

    public MainCommand(PrivateMines plugin) {
        this.plugin = plugin;
        this.teleportCommand = new TeleportCommand(plugin);
        this.resetCommand = new ResetCommand(plugin);
        this.adminResetCommand = new AdminResetCommand(plugin);
        this.setLevelCommand = new SetLevelCommand(plugin);
        this.setBlockCommand = new SetBlockCommand(plugin);
        this.deleteCommand = new DeleteCommand(plugin);
        this.visitCommand = new VisitCommand(plugin);
        this.toggleVisitorCommand = new ToggleVisitorCommand(plugin);
        this.kickCommand = new KickCommand(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("tp")) {
            return teleportCommand.execute(sender, args);
        }

        switch (args[0].toLowerCase()) {
            case "help":
                sendHelpMessage(sender);
                return true;

            case "reset":
                return resetCommand.execute(sender, args);

            case "visit":
                return visitCommand.execute(sender, args);

            case "kick":
                return kickCommand.execute(sender, args);

            case "toggle":
                if (args.length > 1 && args[1].equalsIgnoreCase("visitor")) {
                    return toggleVisitorCommand.execute(sender, args);
                }
                sendHelpMessage(sender);
                return true;

            case "admin":
                if (!sender.hasPermission("privatemines.admin")) {
                    sender.sendMessage(plugin.getConfigManager().getMessage("no_permission"));
                    return true;
                }

                if (args.length > 1 && args[1].equalsIgnoreCase("help")) {
                    sendAdminHelp(sender);
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
        sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "PrivateMines Commands:");
        sender.sendMessage(ChatColor.YELLOW + "/pmine " + ChatColor.GRAY + "- Teleport to your mine");
        sender.sendMessage(ChatColor.YELLOW + "/pmine help " + ChatColor.GRAY + "- Show this help menu");
        sender.sendMessage(ChatColor.YELLOW + "/pmine reset " + ChatColor.GRAY + "- Reset your mine");
        sender.sendMessage(ChatColor.YELLOW + "/pmine visit <player> " + ChatColor.GRAY + "- Visit another player's mine");
        sender.sendMessage(ChatColor.YELLOW + "/pmine kick <player/all> " + ChatColor.GRAY + "- Kick visitors from your mine");
        sender.sendMessage(ChatColor.YELLOW + "/pmine toggle visitor " + ChatColor.GRAY + "- Toggle visitor access to your mine");
    }

    private void sendAdminHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "PrivateMines Admin Commands:");
        sender.sendMessage(ChatColor.YELLOW + "/pmine admin help " + ChatColor.GRAY + "- Show admin commands");
        sender.sendMessage(ChatColor.YELLOW + "/pmine admin reset <player> " + ChatColor.GRAY + "- Reset player's mine");
        sender.sendMessage(ChatColor.YELLOW + "/pmine admin setlevel <player> <level> " + ChatColor.GRAY + "- Set mine level");
        sender.sendMessage(ChatColor.YELLOW + "/pmine admin setblock <player> <identifier> " + ChatColor.GRAY + "- Set mine blocks");
        sender.sendMessage(ChatColor.YELLOW + "/pmine admin delete <player> " + ChatColor.GRAY + "- Delete player's mine");
    }
}