package com.privatemines.managers;

import com.privatemines.PrivateMines;
import com.privatemines.models.MineRegion;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.UUID;

public class WorldGuardManager {

    private final PrivateMines plugin;
    private StateFlag enchantEffectsFlag;

    public WorldGuardManager(PrivateMines plugin) {
        this.plugin = plugin;
        registerCustomFlags();
    }

    private void registerCustomFlags() {
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
        try {
            StateFlag flag = new StateFlag("enchantcore-effects", true);
            registry.register(flag);
            enchantEffectsFlag = flag;
        } catch (Exception e) {
            // Flag might already exist
            enchantEffectsFlag = (StateFlag) registry.get("enchantcore-effects");
        }
    }

    public void createMineRegion(UUID playerUuid, MineRegion mineRegion) {
        if (!Bukkit.getPluginManager().isPluginEnabled("WorldGuard")) {
            return;
        }

        try {
            World world = mineRegion.getWorld();
            RegionManager regionManager = com.sk89q.worldguard.WorldGuard.getInstance()
                    .getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));

            if (regionManager == null) return;

            String regionName = "mine_" + playerUuid.toString();

            BlockVector3 min = BlockVector3.at(mineRegion.getMinX(), mineRegion.getMinY(), mineRegion.getMinZ());
            BlockVector3 max = BlockVector3.at(mineRegion.getMaxX(), mineRegion.getMaxY(), mineRegion.getMaxZ());

            ProtectedCuboidRegion region = new ProtectedCuboidRegion(regionName, min, max);

            // Set enchantcore-effects flag to ALLOW
            if (enchantEffectsFlag != null) {
                region.setFlag(enchantEffectsFlag, StateFlag.State.ALLOW);
            }

            regionManager.addRegion(region);
            plugin.getLogger().info("Created WorldGuard region for mine: " + regionName);

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to create WorldGuard region: " + e.getMessage());
        }
    }
}