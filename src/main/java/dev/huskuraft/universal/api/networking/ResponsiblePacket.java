package dev.huskuraft.universal.api.networking;

import java.util.UUID;

public interface ResponsiblePacket<T extends PacketListener> extends Packet<T> {

    UUID responseId();

}
