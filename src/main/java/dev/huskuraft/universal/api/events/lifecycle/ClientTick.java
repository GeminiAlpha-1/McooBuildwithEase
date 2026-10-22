package dev.huskuraft.universal.api.events.lifecycle;

import dev.huskuraft.universal.api.platform.Client;

@FunctionalInterface
public interface ClientTick {
    void onClientTick(Client client, Phase phase);

    enum Phase {
        START,
        END
    }
}
