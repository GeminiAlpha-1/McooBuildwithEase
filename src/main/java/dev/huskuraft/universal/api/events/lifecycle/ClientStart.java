package dev.huskuraft.universal.api.events.lifecycle;

import dev.huskuraft.universal.api.platform.Client;

@FunctionalInterface
public interface ClientStart {
    void onClientStart(Client client);
}
