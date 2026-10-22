package dev.huskuraft.universal.api.networking;

public interface NetByteBufReader<T> {

    T read(NetByteBuf byteBuf);

}
