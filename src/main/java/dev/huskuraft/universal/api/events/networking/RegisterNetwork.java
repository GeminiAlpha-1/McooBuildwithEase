package dev.huskuraft.universal.api.events.networking;

import dev.huskuraft.universal.api.networking.NetworkRegistry;

@FunctionalInterface
public interface RegisterNetwork {
    void onRegisterNetwork(NetworkRegistry registry);
}
