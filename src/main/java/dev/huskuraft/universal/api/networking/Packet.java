package dev.huskuraft.universal.api.networking;

import dev.huskuraft.universal.api.core.Player;

public interface Packet<T extends PacketListener> {

    void handle(T packetListener, Player sender);

}
