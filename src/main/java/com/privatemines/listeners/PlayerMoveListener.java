package com.privatemines.listeners;

import com.privatemines.PrivateMines;
import com.privatemines.models.MineRegion;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.UUID;

public class PlayerMoveListener implements Listener {

    private final PrivateMines plugin;

    public PlayerMoveListener(PrivateMines plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Location to = event.getTo();
        if (to == null) return;

        Player player = event.getPlayer();
        UUID mineOwner = plugin.getMineManager().getMineOwner(to);

        if (mineOwner == null) return;
        if (mineOwner.equals(player.getUniqueId())) return;
        if (player.hasPermission("privatemines.bypass")) return;

        MineRegion region = plugin.getMineManager().getMineRegion(mineOwner);
        if (region == null) return;

        if (region.isInMineRegion(to)) {
            Location safe = findSafeLocation(to);
            if (safe != null) {
                event.setTo(safe);
                player.sendMessage("§cYou cannot enter another player's mine!");
            }
        }
    }

    private Location findSafeLocation(Location dangerous) {
        Location safe = dangerous.clone();
        safe.setY(dangerous.getY() + 3);

        while (safe.getBlock().getType() != Material.AIR && safe.getY() < 256) {
            safe.setY(safe.getY() + 1);
        }

        return safe.getBlock().getType() == Material.AIR ? safe : null;
    }
}