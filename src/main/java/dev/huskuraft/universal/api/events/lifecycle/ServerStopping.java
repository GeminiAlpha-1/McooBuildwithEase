package dev.huskuraft.universal.api.events.lifecycle;

import dev.huskuraft.universal.api.platform.Server;

@FunctionalInterface
public interface ServerStopping {
    void onServerStopping(Server server);
}
