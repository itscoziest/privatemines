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

        // Send initial title
        sendTitle(player, title, subtitle);

        // Start loading session
        LoadingSession session = new LoadingSession();
        activeSessions.put(playerId, session);

        // Start sound loop if enabled
        if (soundEnabled) {
            startSoundLoop(player, session);
        }

        // Start subtitle animation
        startSubtitleAnimation(player, session);

        DebugUtils.debug("Started loading experience for " + player.getName());
    }

    /**
     * Stop loading experience for a player
     */
    public void stopLoading(Player player) {
        UUID playerId = player.getUniqueId();
        LoadingSession session = activeSessions.remove(playerId);

        if (session != null) {
            // Cancel all tasks
            if (session.soundTask != null && !session.soundTask.isCancelled()) {
                session.soundTask.cancel();
            }
            if (session.subtitleTask != null && !session.subtitleTask.isCancelled()) {
                session.subtitleTask.cancel();
            }

            // Clear title/subtitle
            clearTitle(player);

            DebugUtils.debug("Stopped loading experience for " + player.getName());
        }
    }

    /**
     * Start the suspense sound loop (Hypixel-style) - Faster tempo
     */
    private void startSoundLoop(Player player, LoadingSession session) {
        // Get sound config
        float volume = (float) plugin.getConfigManager().getConfig().getDouble("loading.sound.volume", 0.5);
        float pitchHigh = (float) plugin.getConfigManager().getConfig().getDouble("loading.sound.pitch_high", 1.8);
        float pitchLow = (float) plugin.getConfigManager().getConfig().getDouble("loading.sound.pitch_low", 1.4);

        session.soundTask = new BukkitRunnable() {
            private boolean highPitch = true;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    this.cancel();
                    return;
                }

                // Alternate between high and low pitch notes for suspense effect
                float pitch = highPitch ? pitchHigh : pitchLow;

                // Use note block sounds for that classic Minecraft/Hypixel feel
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, volume, pitch);

                // Switch pitch for next iteration
                highPitch = !highPitch;
            }
        }.runTaskTimer(plugin, 0L, 6L); // Faster: Every 0.3 seconds (6 ticks) instead of 0.5
    }

    /**
     * Start simple static subtitle (no dots animation)
     */
    private void startSubtitleAnimation(Player player, LoadingSession session) {
        // Just send static subtitle once - no animation needed
        String title = plugin.getConfigManager().getMessage("loading_title");
        String subtitle = plugin.getConfigManager().getMessage("loading_subtitle");
        sendTitle(player, title, subtitle);

        // Optional: Update title every few seconds to keep it visible
        session.subtitleTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    this.cancel();
                    return;
                }

                // Just refresh the same title/subtitle
                String title = plugin.getConfigManager().getMessage("loading_title");
                String subtitle = plugin.getConfigManager().getMessage("loading_subtitle");
                sendTitle(player, title, subtitle);
            }
        }.runTaskTimer(plugin, 40L, 40L); // Refresh every 2 seconds
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
        BukkitTask soundTask;
        BukkitTask subtitleTask;
    }
}