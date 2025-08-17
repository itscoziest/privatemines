package com.privatemines.models;

import java.util.UUID;

public class PlayerMine {

    private final UUID playerUuid;
    private boolean isResetting;
    private long lastResetTime;
    private int blocksMinedSinceReset;
    private boolean autoResetEnabled;
    private int autoResetPercentage;

    public PlayerMine(UUID playerUuid) {
        this.playerUuid = playerUuid;
        this.isResetting = false;
        this.lastResetTime = System.currentTimeMillis();
        this.blocksMinedSinceReset = 0;
        this.autoResetEnabled = false;
        this.autoResetPercentage = 95;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public boolean isResetting() {
        return isResetting;
    }

    public void setResetting(boolean resetting) {
        this.isResetting = resetting;
        if (resetting) {
            this.blocksMinedSinceReset = 0;
            this.lastResetTime = System.currentTimeMillis();
        }
    }

    public long getLastResetTime() {
        return lastResetTime;
    }

    public int getBlocksMinedSinceReset() {
        return blocksMinedSinceReset;
    }

    public void incrementBlocksMined() {
        this.blocksMinedSinceReset++;
    }

    public void resetBlockCount() {
        this.blocksMinedSinceReset = 0;
    }

    public boolean isAutoResetEnabled() {
        return autoResetEnabled;
    }

    public void setAutoResetEnabled(boolean autoResetEnabled) {
        this.autoResetEnabled = autoResetEnabled;
    }

    public int getAutoResetPercentage() {
        return autoResetPercentage;
    }

    public void setAutoResetPercentage(int autoResetPercentage) {
        this.autoResetPercentage = Math.max(1, Math.min(100, autoResetPercentage));
    }

    public long getTimeSinceLastReset() {
        return System.currentTimeMillis() - lastResetTime;
    }
}