package dev.huskuraft.universal.api.networking;

import dev.huskuraft.universal.api.core.Player;
import io.netty.buffer.ByteBuf;

public interface ByteBufReceiver {

    void receiveBuffer(ByteBuf byteBuf, Player player);

}
