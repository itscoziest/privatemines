package com.privatemines.managers;

import com.privatemines.PrivateMines;
import com.privatemines.models.MineRegion;
import com.privatemines.utils.DebugUtils;
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

    // Fixed offsets from paste origin (where you stood when copying the schematic)
    // Since you were standing IN the sea lantern, spawn should be at paste origin
    private static final int SPAWN_OFFSET_X = 0;     // You were standing here
    private static final int SPAWN_OFFSET_Y = 0;     // You were standing here
    private static final int SPAWN_OFFSET_Z = 0;     // You were standing here

    // Calculate offsets from where you stood (10,51,224) to actual block positions
    private static final int GOLD_OFFSET_X = -99;    // -89 - 10 = -99
    private static final int GOLD_OFFSET_Y = -1;     // 50 - 51 = -1
    private static final int GOLD_OFFSET_Z = -74;    // 150 - 224 = -74

    private static final int EMERALD_OFFSET_X = 97;   // 107 - 10 = 97
    private static final int EMERALD_OFFSET_Y = -76;  // -25 - 51 = -76
    private static final int EMERALD_OFFSET_Z = -270; // -46 - 224 = -270

    // Plot area identifiers
    private static final int LAPIS_OFFSET_X = -35;    // -25 - 10 = -35
    private static final int LAPIS_OFFSET_Y = 0;      // 52 - 51 - 1 = 0 (lowered by 1)
    private static final int LAPIS_OFFSET_Z = 73;     // 297 - 224 = 73

    private static final int NETHERITE_OFFSET_X = 35;  // 45 - 10 = 35
    private static final int NETHERITE_OFFSET_Y = 50;  // 102 - 51 - 1 = 50 (lowered by 1)
    private static final int NETHERITE_OFFSET_Z = 3;   // 227 - 224 = 3

    public SchematicManager(PrivateMines plugin) {
        this.plugin = plugin;
    }

    public CompletableFuture<MineRegion> pasteSchematic(Location pasteOrigin) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Paste schematic
                File schematicFile = getSchematicFile();
                ClipboardFormat format = ClipboardFormats.findByFile(schematicFile);

                Clipboard clipboard;
                try (ClipboardReader reader = format.getReader(new FileInputStream(schematicFile))) {
                    clipboard = reader.read();
                }

                com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(pasteOrigin.getWorld());
                BlockVector3 to = BlockVector3.at(pasteOrigin.getX(), pasteOrigin.getY(), pasteOrigin.getZ());

                try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
                    editSession.setFastMode(true);

                    Operation operation = new ClipboardHolder(clipboard)
                            .createPaste(editSession)
                            .to(to)
                            .ignoreAirBlocks(true)
                            .build();

                    Operations.complete(operation);
                }

                plugin.getLogger().info("Schematic pasted successfully at " + pasteOrigin);

                // Wait briefly for blocks to settle
                Thread.sleep(500);

                // Calculate regions using fixed offsets (INSTANT)
                return calculateRegionsFromOffsets(pasteOrigin);

            } catch (Exception e) {
                plugin.getLogger().severe("Schematic paste failed: " + e.getMessage());
                return null;
            }
        });
    }

    private MineRegion calculateRegionsFromOffsets(Location pasteOrigin) {
        World world = pasteOrigin.getWorld();

        // Calculate exact positions using fixed offsets
        Location spawnLoc = pasteOrigin.clone().add(SPAWN_OFFSET_X + 0.5, SPAWN_OFFSET_Y + 1, SPAWN_OFFSET_Z + 0.5);
        Location goldLoc = pasteOrigin.clone().add(GOLD_OFFSET_X, GOLD_OFFSET_Y, GOLD_OFFSET_Z);
        Location emeraldLoc = pasteOrigin.clone().add(EMERALD_OFFSET_X, EMERALD_OFFSET_Y, EMERALD_OFFSET_Z);
        Location lapisLoc = pasteOrigin.clone().add(LAPIS_OFFSET_X, LAPIS_OFFSET_Y, LAPIS_OFFSET_Z);
        Location netheriteLoc = pasteOrigin.clone().add(NETHERITE_OFFSET_X, NETHERITE_OFFSET_Y, NETHERITE_OFFSET_Z);

        DebugUtils.debugf("Offset calculation for paste origin: %s", pasteOrigin);
        DebugUtils.debugf("Spawn location: %s", spawnLoc);
        DebugUtils.debugf("Gold location: %s", goldLoc);
        DebugUtils.debugf("Emerald location: %s", emeraldLoc);
        DebugUtils.debugf("Lapis location: %s", lapisLoc);
        DebugUtils.debugf("Netherite location: %s", netheriteLoc);

        // Calculate mining area bounds (between gold and emerald)
        int miningMinX = Math.min(goldLoc.getBlockX(), emeraldLoc.getBlockX());
        int miningMinY = Math.min(goldLoc.getBlockY(), emeraldLoc.getBlockY());
        int miningMinZ = Math.min(goldLoc.getBlockZ(), emeraldLoc.getBlockZ());
        int miningMaxX = Math.max(goldLoc.getBlockX(), emeraldLoc.getBlockX());
        int miningMaxY = Math.max(goldLoc.getBlockY(), emeraldLoc.getBlockY());
        int miningMaxZ = Math.max(goldLoc.getBlockZ(), emeraldLoc.getBlockZ());

        // Calculate plot area bounds (between lapis and netherite)
        int plotMinX = Math.min(lapisLoc.getBlockX(), netheriteLoc.getBlockX());
        int plotMinY = Math.min(lapisLoc.getBlockY(), netheriteLoc.getBlockY());
        int plotMinZ = Math.min(lapisLoc.getBlockZ(), netheriteLoc.getBlockZ());
        int plotMaxX = Math.max(lapisLoc.getBlockX(), netheriteLoc.getBlockX());
        int plotMaxY = Math.max(lapisLoc.getBlockY(), netheriteLoc.getBlockY());
        int plotMaxZ = Math.max(lapisLoc.getBlockZ(), netheriteLoc.getBlockZ());

        DebugUtils.debugf("Mining area: (%d,%d,%d) to (%d,%d,%d)",
                miningMinX, miningMinY, miningMinZ, miningMaxX, miningMaxY, miningMaxZ);
        DebugUtils.debugf("Plot area: (%d,%d,%d) to (%d,%d,%d)",
                plotMinX, plotMinY, plotMinZ, plotMaxX, plotMaxY, plotMaxZ);

        // Remove identifier blocks immediately (using fixed positions)
        final Location finalGoldLoc = goldLoc;
        final Location finalEmeraldLoc = emeraldLoc;
        final Location finalLapisLoc = lapisLoc;
        final Location finalNetheriteLoc = netheriteLoc;

        Bukkit.getScheduler().runTask(plugin, () -> {
            finalGoldLoc.getBlock().setType(Material.AIR);
            finalEmeraldLoc.getBlock().setType(Material.AIR);
            finalLapisLoc.getBlock().setType(Material.AIR);
            finalNetheriteLoc.getBlock().setType(Material.AIR);
            DebugUtils.debug("Removed all identifier blocks");
        });

        // Create MineRegion with calculated bounds
        return new MineRegion(
                world,
                miningMinX, miningMinY, miningMinZ,
                miningMaxX, miningMaxY, miningMaxZ,
                plotMinX, plotMinY, plotMinZ,
                plotMaxX, plotMaxY, plotMaxZ,
                spawnLoc
        );
    }

    private File getSchematicFile() {
        return new File(plugin.getDataFolder(), "schematics/" + plugin.getConfigManager().getDefaultSchematic());
    }
}