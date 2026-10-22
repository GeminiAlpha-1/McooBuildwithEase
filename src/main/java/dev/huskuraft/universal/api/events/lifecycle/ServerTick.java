package dev.huskuraft.universal.api.events.lifecycle;

import dev.huskuraft.universal.api.platform.Server;

@FunctionalInterface
public interface ServerTick {
    void onServerTick(Server server, Phase phase);

    enum Phase {
        START,
        END
    }
}
