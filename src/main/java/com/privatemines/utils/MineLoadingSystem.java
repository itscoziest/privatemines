package com.privatemines.utils;

import com.privatemines.PrivateMines;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MineLoadingSystem {

    private final PrivateMines plugin;

    // Track loading tasks for each player
    private final Map<UUID, LoadingSession> activeSessions = new HashMap<>();

    public MineLoadingSystem(PrivateMines plugin) {
        this.plugin = plugin;
    }

    /**
     * Start loading experience for a player
     */
    public void startLoading(Player player) {
        UUID playerId = player.getUniqueId();

        // Stop any existing loading session
        stopLoading(player);

        // Get config values
        String title = plugin.getConfigManager().getMessage("loading_title");
        String subtitle = plugin.getConfigManager().getMessage("loading_subtitle");
        boolean soundEnabled = plugin.getConfigManager().getConfig().getBoolean("loading.sound.enabled", true);

        sendTitle(player, title, subtitle);

// Play level up sound ONCE
        if (soundEnabled) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }

// Create session (for tracking, but no tasks)
        LoadingSession session = new LoadingSession();
        activeSessions.put(playerId, session);

        DebugUtils.debug("Started loading experience for " + player.getName());
    }

    /**
     * Stop loading experience for a player
     */
    public void stopLoading(Player player) {
        UUID playerId = player.getUniqueId();
        LoadingSession session = activeSessions.remove(playerId);

        if (session != null) {
            // Clear title/subtitle
            clearTitle(player);

            DebugUtils.debug("Stopped loading experience for " + player.getName());
        }
    }


    /**
     * Send title and subtitle to player
     */
    private void sendTitle(Player player, String title, String subtitle) {
        // Convert color codes
        title = MessageUtils.colorize(title);
        subtitle = MessageUtils.colorize(subtitle);

        // Send title with timings: fadeIn=10, stay=100, fadeOut=10 (in ticks)
        player.sendTitle(title, subtitle, 10, 100, 10);
    }

    /**
     * Clear title and subtitle
     */
    private void clearTitle(Player player) {
        player.sendTitle("", "", 0, 1, 0);
    }

    /**
     * Check if player has active loading session
     */
    public boolean isLoading(UUID playerId) {
        return activeSessions.containsKey(playerId);
    }

    /**
     * Data class to hold loading session info
     */
    private static class LoadingSession {
    }
}