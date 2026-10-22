package dev.huskuraft.universal.api.networking;

import java.util.function.Consumer;

import dev.huskuraft.universal.api.core.Player;

public interface PacketSender extends ByteBufSender {

    void sendPacket(Packet packet, Player player);

    default void sendPacket(Packet packet) {
        sendPacket(packet, null);
    }

    default <T extends ResponsiblePacket<?>> void sendPacket(T packet, Consumer<T> callback) {
        sendPacket(packet);
    }


}
