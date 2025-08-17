package com.privatemines.utils;

import com.privatemines.PrivateMines;
import com.privatemines.models.MineData;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Sound;

public class VisitorSystem {

    private final PrivateMines plugin;
    private final Map<UUID, Boolean> visitorSettings = new HashMap<>(); // mine owner -> visitors allowed
    private final Map<UUID, UUID> currentVisitors = new HashMap<>(); // visitor -> mine owner they're visiting

    public VisitorSystem(PrivateMines plugin) {
        this.plugin = plugin;
    }

    /**
     * Toggle visitor access for a mine
     */
    public boolean toggleVisitors(UUID mineOwner) {
        boolean newSetting = !visitorSettings.getOrDefault(mineOwner, false);
        visitorSettings.put(mineOwner, newSetting);

        // If disabling visitors, kick out current visitors (but not the owner!)
        if (!newSetting) {
            currentVisitors.entrySet().removeIf(entry -> {
                if (entry.getValue().equals(mineOwner) && !entry.getKey().equals(mineOwner)) {
                    Player visitor = plugin.getServer().getPlayer(entry.getKey());
                    if (visitor != null && visitor.isOnline()) {
                        visitor.teleport(visitor.getWorld().getSpawnLocation());
                        visitor.sendMessage(plugin.getConfigManager().getMessage("visitor_kicked"));
                    }
                    return true; // Remove from map
                }
                return false;
            });
        }

        DebugUtils.debugf("Visitor access %s for mine owner %s",
                newSetting ? "enabled" : "disabled", mineOwner);

        return newSetting;
    }

    /**
     * Check if mine allows visitors
     */
    public boolean allowsVisitors(UUID mineOwner) {
        return visitorSettings.getOrDefault(mineOwner, false);
    }

    /**
     * Visit another player's mine
     */
    public boolean visitMine(Player visitor, String targetPlayerName) {
        // Find target player's mine
        Player targetPlayer = plugin.getServer().getPlayer(targetPlayerName);
        UUID targetUUID = null;

        // Try online player first
        if (targetPlayer != null) {
            targetUUID = targetPlayer.getUniqueId();
        } else {
            // Try offline player (basic implementation)
            targetUUID = plugin.getServer().getOfflinePlayer(targetPlayerName).getUniqueId();
        }

        MineData targetMine = plugin.getMineManager().getMineData(targetUUID);
        if (targetMine == null) {
            return false; // No mine found
        }

        // Check if visitors are allowed
        if (!allowsVisitors(targetUUID)) {
            return false; // Visitors not allowed
        }

        // Can't visit your own mine this way
        if (visitor.getUniqueId().equals(targetUUID)) {
            return false;
        }

        // Remove from previous visit if any
        currentVisitors.remove(visitor.getUniqueId());

        // Set new visit
        currentVisitors.put(visitor.getUniqueId(), targetUUID);

        // Teleport to target mine
        // Teleport to target mine
        visitor.teleport(targetMine.getLocation());

        Player mineOwner = plugin.getServer().getPlayer(targetUUID);
        if (mineOwner != null && mineOwner.isOnline()) {
            mineOwner.sendMessage(plugin.getConfigManager().getMessage("visitor_joined").replace("{player}", visitor.getName()));

            // Play a "ping"-like sound
            mineOwner.playSound(
                    mineOwner.getLocation(),        // Location to play the sound at
                    Sound.BLOCK_NOTE_BLOCK_PLING,   // The sound enum
                    1.0f,                           // Volume
                    2.0f                            // Pitch (higher = more "ping"-y)
            );
        }


        DebugUtils.debugf("Player %s is now visiting %s's mine",
                visitor.getName(), targetPlayerName);

        return true;
    }

    /**
     * Stop visiting and return to own mine
     */
    public boolean stopVisiting(Player visitor) {
        UUID wasVisiting = currentVisitors.remove(visitor.getUniqueId());
        if (wasVisiting == null) {
            return false; // Wasn't visiting
        }

        // Teleport back to own mine
        MineData ownMine = plugin.getMineManager().getMineData(visitor.getUniqueId());
        if (ownMine != null) {
            visitor.teleport(ownMine.getLocation());
            return true;
        }

        return false;
    }

    /**
     * Check if player is currently visiting someone's mine
     */
    public boolean isVisiting(UUID player) {
        return currentVisitors.containsKey(player);
    }

    /**
     * Get whose mine the player is visiting
     */
    public UUID getVisitingMine(UUID player) {
        return currentVisitors.get(player);
    }

    /**
     * Kick all visitors from a mine
     */
    public int kickAllVisitors(UUID mineOwner) {
        int[] kicked = {0}; // Use array to modify from lambda

        currentVisitors.entrySet().removeIf(entry -> {
            if (entry.getValue().equals(mineOwner)) {
                Player visitor = plugin.getServer().getPlayer(entry.getKey());
                if (visitor != null && visitor.isOnline()) {
                    visitor.teleport(visitor.getWorld().getSpawnLocation());
                    visitor.sendMessage(plugin.getConfigManager().getMessage("visitor_kicked"));
                }
                kicked[0]++;
                return true; // Remove from map
            }
            return false;
        });

        return kicked[0];
    }


    /**
     * Kick a specific visitor
     */
    public boolean kickVisitor(UUID mineOwner, UUID visitor) {
        UUID visitingMine = currentVisitors.get(visitor);
        if (visitingMine != null && visitingMine.equals(mineOwner)) {
            currentVisitors.remove(visitor);

            Player visitorPlayer = plugin.getServer().getPlayer(visitor);
            if (visitorPlayer != null && visitorPlayer.isOnline()) {
                // Teleport to spawn
                visitorPlayer.teleport(visitorPlayer.getWorld().getSpawnLocation());
                visitorPlayer.sendMessage(plugin.getConfigManager().getMessage("visitor_kicked"));
            }
            return true;
        }
        return false;
    }



    /**
     * Get mine owner for border checks (handles visitors)
     */
    public UUID getEffectiveMineOwner(UUID player) {
        // If visiting, return the mine they're visiting
        UUID visiting = currentVisitors.get(player);
        if (visiting != null) {
            return visiting;
        }

        // Otherwise return their own mine
        return player;
    }

    /**
     * Clean up when player leaves
     */
    public void onPlayerQuit(UUID player) {
        currentVisitors.remove(player);
    }
}