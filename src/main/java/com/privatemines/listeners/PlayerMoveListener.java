package com.privatemines.listeners;

import com.privatemines.PrivateMines;
import com.privatemines.utils.MineBorderSystem;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerMoveListener implements Listener {

    private final PrivateMines plugin;
    private final MineBorderSystem borderSystem;

    // Cooldown to prevent spam teleporting (in milliseconds)
    private final Map<UUID, Long> borderCooldowns = new HashMap<>();
    private static final long BORDER_COOLDOWN = 1000; // Reduced to 1 second for better responsiveness

    public PlayerMoveListener(PrivateMines plugin) {
        this.plugin = plugin;
        this.borderSystem = new MineBorderSystem(plugin);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        // Only check if player actually moved to a different block
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
                event.getFrom().getBlockY() == event.getTo().getBlockY() &&
                event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return; // Player didn't move to a new block
        }

        // Only check in mines world
        if (!event.getTo().getWorld().getName().equals(plugin.getConfigManager().getWorldName())) {
            return;
        }

        // Check border violation cooldown
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        Long lastViolation = borderCooldowns.get(playerId);

        if (lastViolation != null && (currentTime - lastViolation) < BORDER_COOLDOWN) {
            return; // Still in cooldown
        }

        // Check if player is within mine borders
        if (!borderSystem.isWithinMineBorders(player, event.getTo())) {
            // Player is outside borders
            borderCooldowns.put(playerId, currentTime);

            // IMMEDIATELY cancel movement to prevent any forward progress
            event.setCancelled(true);

            // Handle violation (teleport back)
            borderSystem.handleBorderViolation(player);
        }
    }
}