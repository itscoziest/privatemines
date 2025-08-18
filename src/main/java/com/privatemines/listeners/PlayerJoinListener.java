package com.privatemines.listeners;

import com.privatemines.PrivateMines;
import com.privatemines.models.MineData;
import com.privatemines.models.MineRegion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private final PrivateMines plugin;

    public PlayerJoinListener(PrivateMines plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            handlePlayerMineLoad(player);
        }, 40L); // 2 second delay to ensure world is loaded
    }

    private void handlePlayerMineLoad(Player player) {
        MineData mineData = plugin.getDataManager().getMineData(player.getUniqueId());

        if (mineData == null) {
            // Player has no mine - nothing to do
            return;
        }

        plugin.getLogger().info("Loading mine data for returning player: " + player.getName());

        // Load their mine region if not already loaded
        MineRegion region = plugin.getMineManager().getMineRegion(player.getUniqueId());
        if (region == null) {
            // Use your existing loadPlayerMineRegion method
            plugin.getMineManager().loadPlayerMineRegion(player.getUniqueId(), mineData);
            plugin.getLogger().info("Reconstructed mine region for: " + player.getName());
        }

        // FIXED: Check if player should be restored to their mine location
        restorePlayerLocationIfNeeded(player, mineData);
    }

    private void restorePlayerLocationIfNeeded(Player player, MineData mineData) {
        Location playerLoc = player.getLocation();
        String minesWorldName = plugin.getConfigManager().getWorldName();

        // Check if player was in mines world when server restarted
        if (playerLoc.getWorld().getName().equals(minesWorldName)) {
            // Player is in mines world - leave them exactly where they are
            plugin.getLogger().info("Player " + player.getName() + " is in mines world - leaving them at current location");
            return;
        }

        // Check if player is at server spawn (indicates they were in mines during restart)
        if (playerLoc.getWorld().getName().equals("world")) { // Main world
            Location serverSpawn = playerLoc.getWorld().getSpawnLocation();
            double distance = playerLoc.distance(serverSpawn);

            if (distance < 10.0) { // Within 10 blocks of spawn
                plugin.getLogger().info("Player " + player.getName() + " is at server spawn - may have been in mines during restart");

                // Restore them to their mine spawn
                final Location mineSpawn = mineData.getLocation();

                // Ensure world is set
                final Location finalMineSpawn;
                if (mineSpawn.getWorld() == null) {
                    finalMineSpawn = new Location(Bukkit.getWorld(minesWorldName), mineSpawn.getX(), mineSpawn.getY(), mineSpawn.getZ());
                } else {
                    finalMineSpawn = mineSpawn;
                }

                // Small delay to ensure mine region is loaded
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    player.teleport(finalMineSpawn);
                    player.sendMessage("§aRestored to your mine (server restart detected)");
                    plugin.getLogger().info("Restored " + player.getName() + " to their mine after restart");
                }, 20L); // 1 second delay
            }
        }
    }
}