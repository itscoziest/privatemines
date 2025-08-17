package com.privatemines.managers;

import com.privatemines.PrivateMines;
import com.privatemines.models.MineRegion;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import java.io.File;
import java.io.FileInputStream;
import java.util.concurrent.CompletableFuture;

public class SchematicManager {

    private final PrivateMines plugin;

    public SchematicManager(PrivateMines plugin) {
        this.plugin = plugin;
    }

    public CompletableFuture<MineRegion> pasteSchematic(Location location) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Paste schematic
                File schematicFile = getSchematicFile();
                ClipboardFormat format = ClipboardFormats.findByFile(schematicFile);

                Clipboard clipboard;
                try (ClipboardReader reader = format.getReader(new FileInputStream(schematicFile))) {
                    clipboard = reader.read();
                }

                com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(location.getWorld());
                BlockVector3 to = BlockVector3.at(location.getX(), location.getY(), location.getZ());

                try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
                    editSession.setFastMode(true);

                    Operation operation = new ClipboardHolder(clipboard)
                            .createPaste(editSession)
                            .to(to)
                            .ignoreAirBlocks(true)
                            .build();

                    Operations.complete(operation);
                }

                plugin.getLogger().info("Schematic pasted successfully");


                // Wait a bit for blocks to settle
                Thread.sleep(1000);

                // Find markers quickly
                return findMarkers(location);


            } catch (Exception e) {
                plugin.getLogger().severe("Schematic paste failed: " + e.getMessage());
                return null;
            }
        });
    }

    private static final int GOLD_OFFSET_X = -259;  // 829 - 1088
    private static final int GOLD_OFFSET_Y = -77;   // 29 - 106
    private static final int GOLD_OFFSET_Z = -74;   // 1198 - 1272
    private static final int EMERALD_OFFSET_X = 239; // 1327 - 1088
    private static final int EMERALD_OFFSET_Y = -1;  // 105 - 106
    private static final int EMERALD_OFFSET_Z = -572; // 700 - 1272

    private static final int GRASS_MIN_X = -35;  // 1053 - 1088
    private static final int GRASS_MIN_Y = 0;    // 106 - 106
    private static final int GRASS_MIN_Z = 3;    // 1275 - 1272
    private static final int GRASS_MAX_X = 35;   // 1123 - 1088
    private static final int GRASS_MAX_Y = 0;    // 106 - 106
    private static final int GRASS_MAX_Z = 73;   // 1345 - 1272

    private MineRegion findMarkers(Location pasteOrigin) {
        World world = pasteOrigin.getWorld();

        Location goldLoc = pasteOrigin.clone().add(GOLD_OFFSET_X, GOLD_OFFSET_Y, GOLD_OFFSET_Z);
        Location emeraldLoc = pasteOrigin.clone().add(EMERALD_OFFSET_X, EMERALD_OFFSET_Y, EMERALD_OFFSET_Z);
        Location spawnLoc = pasteOrigin.clone().add(0.5, 1, 0.5);

        // Remove the markers
        Bukkit.getScheduler().runTask(plugin, () -> {
            goldLoc.getBlock().setType(Material.AIR);
            emeraldLoc.getBlock().setType(Material.AIR);
        });

        plugin.getLogger().info("Gold marker: " + goldLoc);
        plugin.getLogger().info("Emerald marker: " + emeraldLoc);
        plugin.getLogger().info("Spawn point: " + spawnLoc);

        plugin.getLogger().info("Gold marker: " + goldLoc);
        plugin.getLogger().info("Emerald marker: " + emeraldLoc);
        plugin.getLogger().info("Spawn point: " + spawnLoc);
        plugin.getLogger().info("Region bounds: " + goldLoc.getBlockX() + "," + goldLoc.getBlockY() + "," + goldLoc.getBlockZ() +
                " to " + emeraldLoc.getBlockX() + "," + emeraldLoc.getBlockY() + "," + emeraldLoc.getBlockZ());

        return new MineRegion(
                world,
                goldLoc.getBlockX(), goldLoc.getBlockY(), goldLoc.getBlockZ(),
                emeraldLoc.getBlockX(), emeraldLoc.getBlockY(), emeraldLoc.getBlockZ(),
                spawnLoc
        );
    }

    private File getSchematicFile() {
        return new File(plugin.getDataFolder(), "schematics/" + plugin.getConfigManager().getDefaultSchematic());
    }
}