package dev.huskuraft.universal.api.networking;

public interface NetByteBufWriter<T> {

    void write(NetByteBuf byteBuf, T t);

}
