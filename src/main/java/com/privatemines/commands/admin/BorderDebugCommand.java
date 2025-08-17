package com.privatemines.commands.admin;

import com.privatemines.PrivateMines;
import com.privatemines.models.MineData;
import com.privatemines.utils.MineBorderSystem;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BorderDebugCommand {

    private final PrivateMines plugin;

    public BorderDebugCommand(PrivateMines plugin) {
        this.plugin = plugin;
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        Player player = (Player) sender;
        MineData mineData = plugin.getMineManager().getMineData(player.getUniqueId());

        if (mineData == null) {
            player.sendMessage("§cYou don't have a mine!");
            return true;
        }

        Location spawn = mineData.getLocation();
        MineBorderSystem borderSystem = new MineBorderSystem(plugin);
        MineBorderSystem.BorderBounds bounds = borderSystem.getBorderBounds(spawn);

        player.sendMessage("§6=== MINE BORDER DEBUG ===");
        player.sendMessage("§eSpawn (Sea Lantern): §f" + spawn.getBlockX() + ", " + spawn.getBlockY() + ", " + spawn.getBlockZ());
        player.sendMessage("§eBorder Bounds:");
        player.sendMessage("§7  X: §f" + bounds.minX + " §7to §f" + bounds.maxX + " §7(West " + (spawn.getBlockX() - bounds.minX) + " / East " + (bounds.maxX - spawn.getBlockX()) + ")");
        player.sendMessage("§7  Y: §f" + bounds.minY + " §7to §f" + bounds.maxY + " §7(Down " + (spawn.getBlockY() - bounds.minY) + " / Up " + (bounds.maxY - spawn.getBlockY()) + ")");
        player.sendMessage("§7  Z: §f" + bounds.minZ + " §7to §f" + bounds.maxZ + " §7(South " + (spawn.getBlockZ() - bounds.minZ) + " / North " + (bounds.maxZ - spawn.getBlockZ()) + ")");

        Location playerLoc = player.getLocation();
        boolean withinBounds = borderSystem.isWithinMineBorders(player, playerLoc);

        player.sendMessage("§eCurrent Position: §f" + playerLoc.getBlockX() + ", " + playerLoc.getBlockY() + ", " + playerLoc.getBlockZ());
        player.sendMessage("§eWithin Bounds: " + (withinBounds ? "§aYES" : "§cNO"));

        if (!withinBounds) {
            player.sendMessage("§cYou are outside your mine borders!");
        }

        return true;
    }
}