package com.mcoo.buildwithease;

import dev.huskuraft.universal.api.core.Player;
import dev.huskuraft.universal.api.networking.NetworkChannel;
import dev.huskuraft.universal.api.networking.Packet;
import dev.huskuraft.universal.api.networking.Side;
import com.mcoo.buildwithease.building.config.ClientConfig;
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

public final class McooBuildwithEaseClientNetworkChannel extends NetworkChannel<AllPacketListener> {

    private static final int COMPATIBILITY_VERSION = McooBuildwithEase.PROTOCOL_VERSION;
    private final McooBuildwithEaseClient entrance;
    private final AllPacketListener listener;

    public McooBuildwithEaseClientNetworkChannel(McooBuildwithEaseClient entrance) {
        this(entrance, McooBuildwithEase.DEFAULT_CHANNEL);
    }

    public McooBuildwithEaseClientNetworkChannel(McooBuildwithEaseClient entrance, String name) {
        super(entrance, name, Side.CLIENT);
        this.entrance = entrance;
        this.listener = new ClientPacketListener();

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

    private McooBuildwithEaseClient getEntrance() {
        return entrance;
    }

    @Override
    public void receivePacket(Packet packet, Player player) {
        packet.handle(listener, player);
    }

    @Override
    public int getCompatibilityVersion() {
        return COMPATIBILITY_VERSION;
    }

    private class ClientPacketListener implements AllPacketListener {

        private boolean isValidReceiver() {
            if (getEntrance().getClient() == null) {
                return false;
            }
            if (getEntrance().getClient().getWorld() == null) {
                return false;
            }
            return true;
        }

        @Override
        public void handle(PlayerCommandPacket packet, Player player) {
            switch (packet.action()) {
                case REDO, UNDO -> {
                    // noop
                }
            }
        }

        @Override
        public void handle(PlayerSettingsPacket packet, Player player) {
        }

        @Override
        public void handle(PlayerBuildPacket packet, Player player) {
            if (!isValidReceiver()) {
                return;
            }
            var owner = getEntrance().getClient().getWorld().getPlayer(packet.playerId());
            if (owner == null) {
                return;
            }
            getEntrance().getClient().execute(() -> getEntrance().getStructureBuilder().onContextReceived(owner, packet.context()));
        }

        @Override
        public void handle(PlayerPermissionCheckPacket packet, Player player) {
            McooBuildwithEase.LOGGER.info("[permission] 收到服务端回包 granted={} responseId={}", packet.granted(), packet.responseId());
        }

        @Override
        public void handle(PlayerBuildTooltipPacket packet, Player player) {
            if (!isValidReceiver()) {
                return;
            }
            getEntrance().getClient().execute(() -> getEntrance().getStructureBuilder().onTooltipReceived(player, packet.operationTooltip()));

        }

        @Override
        public void handle(SessionPacket packet, Player player) {
            getEntrance().getClient().execute(() -> getEntrance().getSessionManager().onSession(packet.session(), player));
        }

        @Override
        public void handle(SessionConfigPacket packet, Player player) {
            getEntrance().getClient().execute(() -> getEntrance().getSessionManager().onSessionConfig(packet.sessionConfig(), player));

        }

        @Override
        public void handle(PlayerSnapshotCapturePacket packet, Player player) {
            getEntrance().getConfigStorage().update(config -> new ClientConfig(config.renderConfig(), config.patternConfig(), config.clipboardConfig().appendHistory(packet.snapshot())));
            getEntrance().getClient().execute(() -> getEntrance().getStructureBuilder().onSnapshotCaptured(player, packet.snapshot()));
        }

        @Override
        public void handle(PlayerSnapshotSharePacket packet, Player player) {
            getEntrance().getClient().execute(() -> {


            });
        }
    }

}
