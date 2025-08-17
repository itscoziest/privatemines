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
            // Use the data loader to properly load the mine
            plugin.getMineManager().getDataLoader().loadSingleMine(player.getUniqueId(), mineData);
            plugin.getLogger().info("Reconstructed mine region for: " + player.getName());
        }

        // Check if player was in mines world when server restarted
        Location playerLoc = player.getLocation();
        if (playerLoc.getWorld().getName().equals(plugin.getConfigManager().getWorldName())) {
            // Player was in mines world - teleport them to their mine spawn
            Location mineSpawn = mineData.getLocation();

            // Ensure world is set
            if (mineSpawn.getWorld() == null) {
                mineSpawn = new Location(playerLoc.getWorld(), mineSpawn.getX(), mineSpawn.getY(), mineSpawn.getZ());
            }

            player.teleport(mineSpawn);
            player.sendMessage("§aWelcome back! Teleported to your mine.");
            plugin.getLogger().info("Teleported " + player.getName() + " back to their mine after restart");
        }
    }
}