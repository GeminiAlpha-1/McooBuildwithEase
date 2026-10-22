package com.mcoo.buildwithease;

import java.util.concurrent.atomic.AtomicReference;

import dev.huskuraft.universal.api.platform.Server;
import dev.huskuraft.universal.api.platform.ServerManager;

public final class McooBuildwithEaseServerManager implements ServerManager {

    private final McooBuildwithEase entrance;

    private final AtomicReference<Server> runningServer = new AtomicReference<>();

    private int interactionCooldown = 0;

    public McooBuildwithEaseServerManager(McooBuildwithEase entrance) {
        this.entrance = entrance;

        getEntrance().getEventRegistry().getServerStartedEvent().register(this::onServerStarted);
        getEntrance().getEventRegistry().getServerStoppedEvent().register(this::onServerStarted);
    }

    private McooBuildwithEase getEntrance() {
        return entrance;
    }

    public void onServerStarted(Server server) {
        this.runningServer.set(server);
    }

    public void onServerStopped(Server server) {
        this.runningServer.set(null);
    }

    public Server getRunningServer() {
        return runningServer.get();
    }

}
