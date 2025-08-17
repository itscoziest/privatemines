package com.privatemines.placeholders;

import com.privatemines.PrivateMines;
import com.privatemines.models.MineData;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

public class PrivateMinesExpansion extends PlaceholderExpansion {

    private final PrivateMines plugin;

    public PrivateMinesExpansion(PrivateMines plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "privatemines";
    }

    @Override
    public String getAuthor() {
        return "StrikesDev";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (player == null) return "";

        MineData mineData = plugin.getMineManager().getMineData(player.getUniqueId());
        if (mineData == null) return "No Mine";

        switch (params.toLowerCase()) {
            case "level":
                return String.valueOf(mineData.getLevel());
            case "size":
                return mineData.getSize() + "x" + mineData.getSize();
            case "blocks":
                return mineData.getBlockIdentifier();
            case "world":
                return mineData.getLocation().getWorld().getName();
            case "x":
                return String.valueOf(mineData.getLocation().getBlockX());
            case "y":
                return String.valueOf(mineData.getLocation().getBlockY());
            case "z":
                return String.valueOf(mineData.getLocation().getBlockZ());
            case "has_mine":
                return "Yes";
            default:
                return null;
        }
    }
}