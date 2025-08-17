package com.privatemines.utils;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class MessageUtils {

    public static String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static void sendMessage(Player player, String message) {
        player.sendMessage(colorize(message));
    }

    public static void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                new net.md_5.bungee.api.chat.TextComponent(colorize(message)));
    }

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

    public static String formatNumber(long number) {
        if (number < 1000) return String.valueOf(number);
        if (number < 1000000) return String.format("%.1fK", number / 1000.0);
        if (number < 1000000000) return String.format("%.1fM", number / 1000000.0);
        return String.format("%.1fB", number / 1000000000.0);
    }

    public static String replacePlaceholders(String message, String... replacements) {
        String result = message;
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                result = result.replace(replacements[i], replacements[i + 1]);
            }
        }
        return result;
    }
}