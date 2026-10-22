package com.mcoo.buildwithease.networking.packets.session;

import dev.huskuraft.universal.api.core.Player;
import dev.huskuraft.universal.api.networking.NetByteBuf;
import dev.huskuraft.universal.api.networking.NetByteBufSerializer;
import dev.huskuraft.universal.api.networking.Packet;
import com.mcoo.buildwithease.networking.packets.AllPacketListener;
import com.mcoo.buildwithease.networking.serializer.SessionConfigSerializer;
import com.mcoo.buildwithease.session.config.SessionConfig;

public record SessionConfigPacket(
        SessionConfig sessionConfig
) implements Packet<AllPacketListener> {

    @Override
    public void handle(AllPacketListener packetListener, Player sender) {
        packetListener.handle(this, sender);
    }

    public static class Serializer implements NetByteBufSerializer<SessionConfigPacket> {

        @Override
        public SessionConfigPacket read(NetByteBuf byteBuf) {
            return new SessionConfigPacket(byteBuf.read(new SessionConfigSerializer()));
        }

        @Override
        public void write(NetByteBuf byteBuf, SessionConfigPacket packet) {
            byteBuf.write(packet.sessionConfig(), new SessionConfigSerializer());
        }

    }
}
