package dev.huskuraft.universal.api.networking;

import dev.huskuraft.universal.api.core.ResourceLocation;

public interface NetworkRegistry {

    ByteBufSender register(ResourceLocation channelId, Side side, ByteBufReceiver receiver);

}
