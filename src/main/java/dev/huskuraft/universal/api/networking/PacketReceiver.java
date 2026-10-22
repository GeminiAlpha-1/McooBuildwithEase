package dev.huskuraft.universal.api.networking;

import dev.huskuraft.universal.api.core.Player;

public interface PacketReceiver extends ByteBufReceiver {

    void receivePacket(Packet packet, Player player);

}
