package com.privatemines.managers;

import com.privatemines.PrivateMines;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.Bukkit;
import java.util.concurrent.ExecutionException;


import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class PoolManager {

    private final PrivateMines plugin;
    private final Queue<Location> availableLocations;
    private final Set<Location> usedLocations;
    private int nextX;
    private int nextZ;

    public PoolManager(PrivateMines plugin) {
        this.plugin = plugin;
        this.availableLocations = new LinkedList<>();
        this.usedLocations = new HashSet<>();
        this.nextX = plugin.getConfigManager().getStartX();
        this.nextZ = plugin.getConfigManager().getStartZ();
    }

    public Location getNextLocation() {
        if (!availableLocations.isEmpty()) {
            Location location = availableLocations.poll();
            usedLocations.add(location);
            return location;
        }

        String worldName = plugin.getConfigManager().getWorldName();
        World world = plugin.getServer().getWorld(worldName);

        if (world == null) {
            org.bukkit.WorldCreator creator = new org.bukkit.WorldCreator(worldName);
            creator.environment(org.bukkit.World.Environment.NORMAL);
            creator.type(org.bukkit.WorldType.FLAT);
            creator.generatorSettings("{\"layers\":[{\"block\":\"minecraft:air\",\"height\":1}],\"biome\":\"minecraft:the_void\",\"structures\":{\"structures\":{}}}");
            world = plugin.getServer().createWorld(creator);
        }

        Location newLocation = new Location(world, nextX, 64, nextZ);
        usedLocations.add(newLocation);

        calculateNextCoordinates();
        return newLocation;
    }

    private void calculateNextCoordinates() {
        int spacing = plugin.getConfigManager().getWorldSpacing();

        nextX += spacing;
        if (nextX > 5000) {
            nextX = plugin.getConfigManager().getStartX();
            nextZ += spacing;
        }
    }

    public void returnLocation(Location location) {
        if (usedLocations.remove(location)) {
            availableLocations.offer(location);

            int maxCached = plugin.getConfigManager().getConfig().getInt("pool.max_cached", 100);
            while (availableLocations.size() > maxCached) {
                availableLocations.poll();
            }
        }
    }

    public boolean isLocationInUse(Location location) {
        return usedLocations.contains(location);
    }

    public int getUsedCount() {
        return usedLocations.size();
    }

    public int getAvailableCount() {
        return availableLocations.size();
    }
}