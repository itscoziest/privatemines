package com.privatemines.api;

import com.privatemines.managers.MineManager;
import com.privatemines.models.MineData;
import com.privatemines.models.MineRegion;
import org.bukkit.Location;

import java.util.UUID;

public class PrivateMinesAPI {

    private final MineManager mineManager;

    public PrivateMinesAPI(MineManager mineManager) {
        this.mineManager = mineManager;
    }

    public MineData getMine(UUID player) {
        return mineManager.getMineData(player);
    }

    public int getMineLevel(UUID player) {
        MineData mineData = mineManager.getMineData(player);
        return mineData != null ? mineData.getLevel() : 0;
    }

    public String getBlockIdentifier(UUID player) {
        MineData mineData = mineManager.getMineData(player);
        return mineData != null ? mineData.getBlockIdentifier() : null;
    }

    public MineRegion getMineRegion(UUID player) {
        return mineManager.getMineRegion(player);
    }

    public boolean isInMine(Location location, UUID player) {
        return mineManager.isInMine(location, player);
    }

    public UUID getMineOwner(Location location) {
        return mineManager.getMineOwner(location);
    }

    public boolean hasMine(UUID player) {
        return mineManager.getMineData(player) != null;
    }

    public Location getMineSpawn(UUID player) {
        MineData mineData = mineManager.getMineData(player);
        return mineData != null ? mineData.getLocation() : null;
    }

    public int getMineSize(UUID player) {
        MineData mineData = mineManager.getMineData(player);
        return mineData != null ? mineData.getSize() : 0;
    }
}