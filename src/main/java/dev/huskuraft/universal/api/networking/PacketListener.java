package dev.huskuraft.universal.api.networking;

public interface PacketListener {

    default boolean shouldPropagateHandlingExceptions() {
        return true;
    }

}
