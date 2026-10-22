package dev.huskuraft.universal.api.networking;

import dev.huskuraft.universal.api.core.ResourceLocation;

public interface PacketChannel extends PacketSender, PacketReceiver {

    int getCompatibilityVersion();

    ResourceLocation getChannelId();


}
