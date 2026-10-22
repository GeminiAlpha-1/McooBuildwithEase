package dev.huskuraft.universal.api.networking;

import dev.huskuraft.universal.api.core.Player;
import io.netty.buffer.ByteBuf;

public interface ByteBufSender {

    void sendBuffer(ByteBuf byteBuf, Player player);

}
