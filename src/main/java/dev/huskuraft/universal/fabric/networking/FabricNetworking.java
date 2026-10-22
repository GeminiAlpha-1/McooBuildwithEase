package dev.huskuraft.universal.fabric.networking;

import com.google.auto.service.AutoService;
import dev.huskuraft.universal.api.core.Player;
import dev.huskuraft.universal.api.core.ResourceLocation;
import dev.huskuraft.universal.api.networking.ByteBufReceiver;
import dev.huskuraft.universal.api.networking.ByteBufSender;
import dev.huskuraft.universal.api.networking.Networking;
import dev.huskuraft.universal.api.networking.Side;
import dev.huskuraft.universal.vanilla.core.MinecraftPlayer;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

@AutoService(Networking.class)
public class FabricNetworking implements Networking {

    public static final Map<ResourceLocation, Payload.Type<Payload>> MAP = new HashMap<>();

    public static ByteBufSender register(ResourceLocation channelId, Side side, ByteBufReceiver receiver) {
        var payloadType = MAP.computeIfAbsent(channelId, resourceLocation -> {
            var type = new Payload.Type<Payload>(resourceLocation.reference());
            var codec = new StreamCodec<RegistryFriendlyByteBuf, Payload>() {
                @Override
                public Payload decode(RegistryFriendlyByteBuf byteBuf) {
                    return new Payload(type, byteBuf.readBytes(byteBuf.readableBytes()));
                }
                @Override
                public void encode(RegistryFriendlyByteBuf byteBuf, Payload payload) {
                    byteBuf.writeBytes(payload.byteBuf());
                }
            };
            // 26.2 的 Fabric API 改了注册入口名：playC2S() → serverboundPlay()，playS2C() → clientboundPlay()
            PayloadTypeRegistry.serverboundPlay().register(type, codec);
            PayloadTypeRegistry.clientboundPlay().register(type, codec);
            return type;
        });
        var fabricSide = switch (side) {
            case CLIENT -> new Client();
            case SERVER -> new Server();
        };
        fabricSide.registerReceiver(payloadType, (payload, player) -> {
            receiver.receiveBuffer(payload.byteBuf(), player);
        });
        return (byteBuf, player) -> {
            fabricSide.send(new Payload(payloadType, byteBuf), player);
        };
    }

    public record Payload(Type<Payload> type, ByteBuf byteBuf) implements CustomPacketPayload {
    }

    void registerReceiver(Payload.Type<Payload> type, BiConsumer<Payload, Player> receiver) {
    }

    void send(Payload payload, Player player) {
    }

    static class Server extends FabricNetworking {

        @Override
        public void registerReceiver(Payload.Type<Payload> type, BiConsumer<Payload, Player> receiver) {
            ServerPlayNetworking.registerGlobalReceiver(type, (payload, context) -> {
                receiver.accept(payload, MinecraftPlayer.ofNullable(context.player()));
            });
        }

        @Override
        public void send(Payload payload, Player player) {
            // 【关键】只给「声明了该 channel 的客户端」发包。
            // 客户端没装本 mod 时不会在登录阶段上报这个 channel，
            // 无条件发送会给原版客户端发一个未知自定义包 —— 26.2 原版虽然会静默丢弃（DiscardedPayload），
            // 但不依赖这种兜底行为更稳，也避免服务端侧的无谓序列化开销。
            var serverPlayer = (net.minecraft.server.level.ServerPlayer) player.reference();
            if (!ServerPlayNetworking.canSend(serverPlayer, payload.type().id())) {
                return;
            }
            ServerPlayNetworking.send(serverPlayer, payload);
        }
    }

    static class Client extends FabricNetworking {

        @Override
        public void registerReceiver(Payload.Type<Payload> type, BiConsumer<Payload, Player> receiver) {
            ClientPlayNetworking.registerGlobalReceiver(type, (payload, context) -> {
                receiver.accept(payload, MinecraftPlayer.ofNullable(context.player()));
            });
        }

        @Override
        public void send(Payload payload, Player player) {
            ClientPlayNetworking.send(payload);
        }
    }

}
