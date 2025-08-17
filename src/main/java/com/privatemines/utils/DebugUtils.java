package com.privatemines.utils;

import com.privatemines.PrivateMines;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DebugUtils {

    private static PrivateMines plugin;

    public static void init(PrivateMines pluginInstance) {
        plugin = pluginInstance;
    }

    /**
     * Check if debug is enabled in config
     */
    public static boolean isDebugEnabled() {
        if (plugin == null) return false;
        return plugin.getConfigManager().getConfig().getBoolean("debug.enabled", false);
    }

    /**
     * Check if console debug is enabled
     */
    public static boolean isConsoleDebugEnabled() {
        if (!isDebugEnabled()) return false;
        return plugin.getConfigManager().getConfig().getBoolean("debug.console", true);
    }

    /**
     * Check if chat debug is enabled
     */
    public static boolean isChatDebugEnabled() {
        if (!isDebugEnabled()) return false;
        return plugin.getConfigManager().getConfig().getBoolean("debug.chat", false);
    }

    /**
     * Log debug message to console only
     */
    public static void logDebug(String message) {
        if (isConsoleDebugEnabled()) {
            plugin.getLogger().info("[DEBUG] " + message);
        }
    }

    /**
     * Send debug message to player (if they have permission and chat debug is enabled)
     */
    public static void sendDebug(Player player, String message) {
        if (isChatDebugEnabled() && player.hasPermission("privatemines.debug")) {
            player.sendMessage("§7[DEBUG] " + message);
        }
    }

    /**
     * Send debug message to command sender (if they have permission and chat debug is enabled)
     */
    public static void sendDebug(CommandSender sender, String message) {
        if (sender instanceof Player) {
            sendDebug((Player) sender, message);
        } else if (isConsoleDebugEnabled()) {
            sender.sendMessage("[DEBUG] " + message);
        }
    }

    /**
     * Log and send debug message (both console and chat if enabled)
     */
    public static void debug(String message) {
        logDebug(message);
    }

    /**
     * Log and send debug message to specific player
     */
    public static void debug(Player player, String message) {
        logDebug(message);
        sendDebug(player, message);
    }

    /**
     * Debug with formatted message
     */
    public static void debugf(String format, Object... args) {
        debug(String.format(format, args));
    }

    /**
     * Debug with formatted message to specific player
     */
    public static void debugf(Player player, String format, Object... args) {
        debug(player, String.format(format, args));
    }
}