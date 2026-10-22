package dev.huskuraft.universal.fabric.events;

import com.google.auto.service.AutoService;

import dev.huskuraft.universal.api.events.impl.EventRegistry;
import dev.huskuraft.universal.fabric.events.common.PlatformLifecycleEvents;
import dev.huskuraft.universal.fabric.networking.FabricNetworking;
import dev.huskuraft.universal.vanilla.core.MinecraftPlayer;
import dev.huskuraft.universal.vanilla.core.MinecraftServer;
import dev.huskuraft.universal.vanilla.core.MinecraftWorld;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@AutoService(EventRegistry.class)
public class FabricEventRegistry extends EventRegistry {

    /**
     * 记录每个玩家最后一次出现的维度，用来区分「换维度」与「原地重生」。
     * 26.2 的 Fabric API 没有换世界事件，也没有重生事件，
     * 两者都只能从 ServerEntityEvents.ENTITY_LOAD 推断（玩家实体被装进某个世界时触发）。
     */
    private final Map<UUID, ServerLevel> lastLevels = new ConcurrentHashMap<>();

    public FabricEventRegistry() {
        PlatformLifecycleEvents.COMMON_START.register(() -> {
            getRegisterNetworkEvent().invoker().onRegisterNetwork(FabricNetworking::register);
        });

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            getServerStartingEvent().invoker().onServerStarting(new MinecraftServer(server));
        });
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            getServerStartedEvent().invoker().onServerStarted(new MinecraftServer(server));
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            getServerStoppingEvent().invoker().onServerStopping(new MinecraftServer(server));
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            getServerStoppedEvent().invoker().onServerStopped(new MinecraftServer(server));
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            var player = handler.player;
            if (player == null) {
                return;
            }
            getPlayerLoggedInEvent().invoker().onPlayerLoggedIn(MinecraftPlayer.ofNullable(player));
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            var player = handler.player;
            if (player == null) {
                return;
            }
            lastLevels.remove(player.getUUID());
            getPlayerLoggedOutEvent().invoker().onPlayerLoggedOut(MinecraftPlayer.ofNullable(player));
        });

        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (!(entity instanceof ServerPlayer player)) {
                return;
            }
            var previous = lastLevels.put(player.getUUID(), world);
            if (previous == null) {
                // 首次进入世界，登录事件已由 ServerPlayConnectionEvents.JOIN 处理
                return;
            }
            if (previous != world) {
                // 换维度
                getPlayerChangeWorldEvent().invoker().onPlayerChangeWorld(
                        MinecraftPlayer.ofNullable(player),
                        MinecraftWorld.ofNullable(previous),
                        MinecraftWorld.ofNullable(world));
            } else {
                // 原地重生：新玩家实体被重新装回同一个世界
                getPlayerRespawnEvent().invoker().onPlayerRespawn(
                        MinecraftPlayer.ofNullable(player),
                        MinecraftPlayer.ofNullable(player),
                        true);
            }
        });
    }

}
