package com.privatemines.models;

import org.bukkit.Location;

import java.util.UUID;

public class MineData {

    private final UUID uuid;
    private int level;
    private String blockIdentifier;
    private Location location;
    private String owner;

    public MineData(UUID uuid, int level, String blockIdentifier, Location location, String owner) {
        this.uuid = uuid;
        this.level = level;
        this.blockIdentifier = blockIdentifier;
        this.location = location;
        this.owner = owner;
    }

    public UUID getUuid() {
        return uuid;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getBlockIdentifier() {
        return blockIdentifier;
    }

    public void setBlockIdentifier(String blockIdentifier) {
        this.blockIdentifier = blockIdentifier;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public int getSize() {
        return getSizeForLevel(level);
    }

    private int getSizeForLevel(int level) {
        switch (level) {
            case 1: return 50;
            case 2: return 75;
            case 3: return 100;
            case 4: return 150;
            case 5: return 200;
            case 6: return 250;
            case 7: return 300;
            case 8: return 350;
            case 9: return 400;
            case 10: return 500;
            default: return 50;
        }
    }
}