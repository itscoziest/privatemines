package com.privatemines.utils;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MessageUtils {

    /**
     * Translates & color codes to § color codes
     */
    public static String colorize(String message) {
        if (message == null) return "";
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Send colorized message to player
     */
    public static void sendMessage(Player player, String message) {
        player.sendMessage(colorize(message));
    }

    /**
     * Send colorized message to any command sender
     */
    public static void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(colorize(message));
    }

    /**
     * Send colorized action bar message
     */
    public static void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                new net.md_5.bungee.api.chat.TextComponent(colorize(message)));
    }

    /**
     * Format time in a readable way
     */
    public static String formatTime(int seconds) {
        if (seconds < 60) {
            return seconds + "s";
        }

        int minutes = seconds / 60;
        int remainingSeconds = seconds % 60;

        if (minutes < 60) {
            return minutes + "m " + remainingSeconds + "s";
        }

        int hours = minutes / 60;
        int remainingMinutes = minutes % 60;
        return hours + "h " + remainingMinutes + "m " + remainingSeconds + "s";
    }

    /**
     * Format large numbers with K, M, B suffixes
     */
    public static String formatNumber(long number) {
        if (number < 1000) return String.valueOf(number);
        if (number < 1000000) return String.format("%.1fK", number / 1000.0);
        if (number < 1000000000) return String.format("%.1fM", number / 1000000.0);
        return String.format("%.1fB", number / 1000000000.0);
    }

    /**
     * Replace placeholders in a message and colorize it
     */
    public static String replacePlaceholders(String message, String... replacements) {
        String result = message;
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                result = result.replace(replacements[i], replacements[i + 1]);
            }
        }
        return colorize(result);
    }

    /**
     * Send a message with placeholder replacements
     */
    public static void sendMessageWithPlaceholders(CommandSender sender, String message, String... replacements) {
        sender.sendMessage(replacePlaceholders(message, replacements));
    }
}