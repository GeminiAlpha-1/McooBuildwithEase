package com.mcoo.buildwithease;

import java.util.concurrent.atomic.AtomicReference;

import dev.huskuraft.universal.api.core.Player;
import dev.huskuraft.universal.api.core.World;
import dev.huskuraft.universal.api.platform.Platform;
import dev.huskuraft.universal.api.platform.Server;
import dev.huskuraft.universal.api.text.Text;
import com.mcoo.buildwithease.networking.packets.session.SessionConfigPacket;
import com.mcoo.buildwithease.networking.packets.session.SessionPacket;
import com.mcoo.buildwithease.session.Session;
import com.mcoo.buildwithease.session.SessionManager;
import com.mcoo.buildwithease.session.config.SessionConfig;

public final class McooBuildwithEaseSessionManager implements SessionManager {

    private final McooBuildwithEase entrance;

    private final AtomicReference<Session> serverSession = new AtomicReference<>();
    private final AtomicReference<Session> clientSession = new AtomicReference<>();

    public McooBuildwithEaseSessionManager(McooBuildwithEase entrance) {
        this.entrance = entrance;

        getEntrance().getEventRegistry().getPlayerChangeWorldEvent().register(this::onPlayerChangeWorld);
        getEntrance().getEventRegistry().getPlayerRespawnEvent().register(this::onPlayerRespawn);
        getEntrance().getEventRegistry().getPlayerLoggedInEvent().register(this::onPlayerLoggedIn);
        getEntrance().getEventRegistry().getPlayerLoggedOutEvent().register(this::onPlayerLoggedOut);
    }

    private McooBuildwithEase getEntrance() {
        return entrance;
    }


    @Override
    public void onSession(Session session, Player player) {
        serverSession.set(session);
    }

    @Override
    public void onSessionConfig(SessionConfig sessionConfig, Player player) {
        if (!player.isOperator() && !player.isSinglePlayerOwner()) {
            player.sendMessage(McooBuildwithEase.getSystemMessage(Text.text("You do not have permission to set server config.")));
            McooBuildwithEase.LOGGER.warn("%s has no permission to set server config.".formatted(player.getProfile().getName()));
            return;
        }

        getEntrance().getSessionConfigStorage().set(sessionConfig);

        Server server = player.getServer();
        for (var serverPlayer : server.getPlayerList().getPlayers()) {
            updateSessionConfig(serverPlayer);
        }
    }

    @Override
    public boolean isSessionValid() {
        return serverSession.get() != null;
    }

    @Override
    public SessionStatus getSessionStatus() {
        if (serverSession.get() == null && clientSession.get() == null) {
            return SessionStatus.MOD_MISSING;
        }
        if (serverSession.get() == null) {
            return SessionStatus.SERVER_MOD_MISSING;
        }
        if (clientSession.get() == null) {
            return SessionStatus.CLIENT_MOD_MISSING;
        }
        var serverMod = serverSession.get().mods().stream().filter(mod -> mod.getId().equals(McooBuildwithEase.MOD_ID)).findFirst().orElseThrow();
        var clientMod = clientSession.get().mods().stream().filter(mod -> mod.getId().equals(McooBuildwithEase.MOD_ID)).findFirst().orElseThrow();

        if (!serverMod.getVersionStr().equals(clientMod.getVersionStr())) {
            return SessionStatus.PROTOCOL_NOT_MATCH;
        }
        return SessionStatus.SUCCESS;
    }

    @Override
    public Session getLastSession() {
        var platform = Platform.getInstance();
        var protocolVersion = McooBuildwithEase.PROTOCOL_VERSION;
        return new Session(
                platform.getLoaderType(),
                platform.getLoaderVersion(),
                platform.getGameVersion(),
                platform.getRunningMods(),
                protocolVersion

        );
    }

    @Override
    public SessionConfig getLastSessionConfig() {
        return getEntrance().getSessionConfigStorage().get();
    }

    private void onPlayerChangeWorld(Player player, World origin, World destination) {
    }

    private void onPlayerRespawn(Player oldPlayer, Player newPlayer, boolean alive) {
    }

    private void onPlayerLoggedIn(Player player) {
        updateSession(player);
        updateSessionConfig(player);
    }

    private void onPlayerLoggedOut(Player player) {
    }

    private void updateSession(Player player) {
        getEntrance().getChannel().sendPacket(new SessionPacket(getLastSession()), player);
    }

    private void updateSessionConfig(Player player) {
        getEntrance().getChannel().sendPacket(new SessionConfigPacket(getLastSessionConfig()), player);
    }

}
