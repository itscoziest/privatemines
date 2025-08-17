package com.privatemines.utils;

import com.privatemines.PrivateMines;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.LinkedList;
import java.util.Queue;

public class OptimizedClearingSystem {

    private final PrivateMines plugin;
    private final Queue<ClearingTask> clearingQueue = new LinkedList<>();
    private boolean isProcessing = false;

    // Performance limits
    private static final int MAX_BLOCKS_PER_TICK = 200; // Reduced for server stability
    private static final int MAX_CONCURRENT_CLEARS = 1;  // Only 1 clearing at a time
    private static final long CLEAR_DELAY_MS = 100;     // 100ms between operations

    public OptimizedClearingSystem(PrivateMines plugin) {
        this.plugin = plugin;
        startProcessor();
    }

    /**
     * Queue a mine area for clearing
     */
    public void queueAreaClear(Location center, String playerName) {
        synchronized (clearingQueue) {
            // Check if already queued
            for (ClearingTask task : clearingQueue) {
                if (task.center.equals(center)) {
                    plugin.getLogger().info("Mine clearing already queued for " + playerName);
                    return;
                }
            }

            clearingQueue.offer(new ClearingTask(center, playerName));
            plugin.getLogger().info("Queued mine clearing for " + playerName + " (Queue size: " + clearingQueue.size() + ")");
        }
    }

    /**
     * Start the centralized processor
     */
    private void startProcessor() {
        new BukkitRunnable() {
            @Override
            public void run() {
                processQueue();
            }
        }.runTaskTimer(plugin, 20L, 20L); // Run every second
    }

    /**
     * Process clearing queue safely
     */
    private void processQueue() {
        // Don't start new clearing if one is active
        if (isProcessing) return;

        synchronized (clearingQueue) {
            if (clearingQueue.isEmpty()) return;

            ClearingTask task = clearingQueue.poll();
            isProcessing = true;

            plugin.getLogger().info("Starting optimized clearing for " + task.playerName +
                    " (Remaining in queue: " + clearingQueue.size() + ")");

            startClearing(task);
        }
    }

    /**
     * Optimized clearing with better performance controls
     */
    private void startClearing(ClearingTask task) {
        new BukkitRunnable() {
            private int currentX = -100; // Smaller area: 200x40x200
            private int currentZ = -100;
            private int blocksCleared = 0;
            private long lastOpTime = System.currentTimeMillis();

            @Override
            public void run() {
                // Throttle operations to prevent lag
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastOpTime < CLEAR_DELAY_MS) {
                    return; // Skip this tick
                }
                lastOpTime = currentTime;

                int processed = 0;

                // Process blocks in smaller chunks
                while (processed < MAX_BLOCKS_PER_TICK && currentX <= 100) {
                    if (currentZ > 100) {
                        currentX += 5; // Skip every 5th block for speed
                        currentZ = -100;
                        continue;
                    }

                    // Only clear essential Y levels
                    for (int y = -5; y <= 35; y += 2) { // Skip every other Y level
                        Location clearLoc = task.center.clone().add(currentX, y, currentZ);

                        // Only clear non-air blocks
                        if (clearLoc.getBlock().getType() != Material.AIR) {
                            clearLoc.getBlock().setType(Material.AIR, false);
                            blocksCleared++;
                        }

                        processed++;
                        if (processed >= MAX_BLOCKS_PER_TICK) break;
                    }

                    if (processed >= MAX_BLOCKS_PER_TICK) break;
                    currentZ += 5; // Skip every 5th block
                }

                // Check completion
                if (currentX > 100) {
                    plugin.getLogger().info("Clearing completed for " + task.playerName +
                            " - " + blocksCleared + " blocks cleared");
                    isProcessing = false;
                    this.cancel();
                    return;
                }

                // Progress update every 10 seconds
                if (blocksCleared % 500 == 0 && blocksCleared > 0) {
                    plugin.getLogger().info("Clearing progress for " + task.playerName + ": " +
                            blocksCleared + " blocks cleared");
                }
            }
        }.runTaskTimer(plugin, 1L, 2L); // Run every 2 ticks for stability
    }

    /**
     * Get current queue status
     */
    public String getQueueStatus() {
        return "Queue: " + clearingQueue.size() + " pending, " +
                (isProcessing ? "1 active" : "0 active");
    }

    /**
     * Emergency queue clear
     */
    public void clearQueue() {
        synchronized (clearingQueue) {
            clearingQueue.clear();
            isProcessing = false;
            plugin.getLogger().info("Clearing queue has been reset");
        }
    }

    /**
     * Data class for clearing tasks
     */
    private static class ClearingTask {
        final Location center;
        final String playerName;

        ClearingTask(Location center, String playerName) {
            this.center = center;
            this.playerName = playerName;
        }
    }
}