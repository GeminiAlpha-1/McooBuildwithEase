package dev.huskuraft.universal.api.events.lifecycle;

import dev.huskuraft.universal.api.platform.Server;

@FunctionalInterface
public interface ServerStopped {
    void onServerStopped(Server server);
}
