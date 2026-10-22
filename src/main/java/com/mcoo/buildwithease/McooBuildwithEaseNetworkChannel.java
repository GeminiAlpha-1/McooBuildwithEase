package com.mcoo.buildwithease;

import java.util.logging.Logger;

import dev.huskuraft.universal.api.core.Player;
import dev.huskuraft.universal.api.networking.NetworkChannel;
import dev.huskuraft.universal.api.networking.Packet;
import dev.huskuraft.universal.api.networking.Side;
import dev.huskuraft.universal.api.text.Text;
import com.mcoo.buildwithease.networking.packets.AllPacketListener;
import com.mcoo.buildwithease.networking.packets.player.PlayerBuildPacket;
import com.mcoo.buildwithease.networking.packets.player.PlayerBuildTooltipPacket;
import com.mcoo.buildwithease.networking.packets.player.PlayerCommandPacket;
import com.mcoo.buildwithease.networking.packets.player.PlayerPermissionCheckPacket;
import com.mcoo.buildwithease.networking.packets.player.PlayerSettingsPacket;
import com.mcoo.buildwithease.networking.packets.player.PlayerSnapshotCapturePacket;
import com.mcoo.buildwithease.networking.packets.player.PlayerSnapshotSharePacket;
import com.mcoo.buildwithease.networking.packets.session.SessionConfigPacket;
import com.mcoo.buildwithease.networking.packets.session.SessionPacket;

public final class McooBuildwithEaseNetworkChannel extends NetworkChannel<AllPacketListener> {

    private static final int COMPATIBILITY_VERSION = McooBuildwithEase.PROTOCOL_VERSION;
    private final McooBuildwithEase entrance;
    private final AllPacketListener listener;

    public McooBuildwithEaseNetworkChannel(McooBuildwithEase entrance) {
        this(entrance, McooBuildwithEase.DEFAULT_CHANNEL);
    }

    public McooBuildwithEaseNetworkChannel(McooBuildwithEase entrance, String name) {
        super(entrance, name, Side.SERVER);
        this.entrance = entrance;
        this.listener = new ServerPacketListener();

        registerPacket(SessionPacket.class, new SessionPacket.Serializer());
        registerPacket(SessionConfigPacket.class, new SessionConfigPacket.Serializer());

        registerPacket(PlayerCommandPacket.class, new PlayerCommandPacket.Serializer());
        registerPacket(PlayerSettingsPacket.class, new PlayerSettingsPacket.Serializer());
        registerPacket(PlayerBuildPacket.class, new PlayerBuildPacket.Serializer());
        registerPacket(PlayerPermissionCheckPacket.class, new PlayerPermissionCheckPacket.Serializer());
        registerPacket(PlayerBuildTooltipPacket.class, new PlayerBuildTooltipPacket.Serializer());
        registerPacket(PlayerSnapshotCapturePacket.class, new PlayerSnapshotCapturePacket.Serializer());
        registerPacket(PlayerSnapshotSharePacket.class, new PlayerSnapshotSharePacket.Serializer());

        getEntrance().getEventRegistry().getRegisterNetworkEvent().register(this::onRegisterNetwork);
    }

    public McooBuildwithEase getEntrance() {
        return entrance;
    }

    @Override
    public void receivePacket(Packet packet, Player player) {
        try {
            packet.handle(listener, player);
        } catch (Exception exception) {
            if (listener.shouldPropagateHandlingExceptions()) {
                throw exception;
            }
            Logger.getAnonymousLogger().severe("Failed to handle packet " + packet + ", suppressing error" + exception);
        }
    }

    @Override
    public int getCompatibilityVersion() {
        return COMPATIBILITY_VERSION;
    }

    private class ServerPacketListener implements AllPacketListener {

        @Override
        public void handle(PlayerCommandPacket packet, Player player) {
            getEntrance().getServer().execute(() -> {
                switch (packet.action()) {
                    case REDO -> getEntrance().getStructureBuilder().redo(player);
                    case UNDO -> getEntrance().getStructureBuilder().undo(player);
                }
            });
        }

        @Override
        public void handle(PlayerSettingsPacket packet, Player player) {
        }

        @Override
        public void handle(PlayerBuildPacket packet, Player player) {
            getEntrance().getServer().execute(() -> {
                getEntrance().getStructureBuilder().onContextReceived(player, packet.context());
            });

        }

        @Override
        public void handle(PlayerPermissionCheckPacket packet, Player player) {
            McooBuildwithEase.LOGGER.info("[permission] 服务端收到权限校验请求 player={}", player.getProfile().getName());
            getEntrance().getServer().execute(() -> {
                var isSinglePlayerOwner = getEntrance().getServerManager().getRunningServer().isSinglePlayerOwner(player.getProfile());
                var isOperator = getEntrance().getServerManager().getRunningServer().isOperator(player.getProfile());
                McooBuildwithEase.LOGGER.info("[permission] owner={} operator={} -> 回包", isSinglePlayerOwner, isOperator);
                getEntrance().getChannel().sendPacket(new PlayerPermissionCheckPacket(packet.responseId(), packet.playerId(), isSinglePlayerOwner || isOperator), player);
            });

        }

        @Override
        public void handle(PlayerBuildTooltipPacket packet, Player player) {

        }

        @Override
        public void handle(SessionPacket packet, Player player) {
            getEntrance().getServer().execute(() -> {
                getEntrance().getSessionManager().onSession(packet.session(), player);
            });
        }

        @Override
        public void handle(SessionConfigPacket packet, Player player) {
            getEntrance().getServer().execute(() -> {
                getEntrance().getSessionManager().onSessionConfig(packet.sessionConfig(), player);
            });
        }

        @Override
        public void handle(PlayerSnapshotCapturePacket packet, Player player) {

        }

        @Override
        public void handle(PlayerSnapshotSharePacket packet, Player player) {
            getEntrance().getServer().execute(() -> {
                var fromPlayer = getEntrance().getServer().getPlayerList().getPlayer(packet.from());
                var toPlayer = getEntrance().getServer().getPlayerList().getPlayer(packet.to());
                if (fromPlayer == null) {
                    return;
                }
                if (toPlayer == null) {
                    fromPlayer.sendMessage(McooBuildwithEase.getSystemMessage(Text.text("Cannot share this snapshot. Player is offline.")));
                } else {
                    fromPlayer.sendMessage(McooBuildwithEase.getSystemMessage(Text.text("Snapshot shared to player %s.".formatted(toPlayer.getProfile().getName()))));
                    toPlayer.sendMessage(McooBuildwithEase.getSystemMessage(Text.text("Player %s shared you a snapshot. Go to clipboard to import it.".formatted(fromPlayer.getProfile().getName()))));
                    getEntrance().getChannel().sendPacket(packet, toPlayer);
                }
            });


        }
    }

}
