package com.privatemines.utils;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * Static utility class for sending ProtocolLib packets
 */
public class PacketUtils {

    /**
     * Sends a fake block change packet to the player
     * @param player The player to send the packet to
     * @param location The location of the block change
     * @param material The material to display (usually AIR for block breaks)
     */
    public static void sendBlockChange(Player player, Location location, Material material) {
        try {
            PacketContainer packet = createBlockChangePacket(location, material);
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
        } catch (Exception e) {
            System.err.println("Failed to send block change packet: " + e.getMessage());
        }
    }

    /**
     * Creates a block change packet
     */
    private static PacketContainer createBlockChangePacket(Location location, Material material) {
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.BLOCK_CHANGE);

        BlockPosition position = new BlockPosition(
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ()
        );

        packet.getBlockPositionModifier().write(0, position);
        packet.getBlockData().write(0, com.comphenix.protocol.wrappers.WrappedBlockData.createData(material));

        return packet;
    }

    /**
     * Sends block change packets to multiple players
     */
    public static void sendBlockChangeToPlayers(Iterable<Player> players, Location location, Material material) {
        try {
            PacketContainer packet = createBlockChangePacket(location, material);
            for (Player player : players) {
                ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
            }
        } catch (Exception e) {
            System.err.println("Failed to send block change packets: " + e.getMessage());
        }
    }
}