package dev.huskuraft.universal.api.events.input;

@FunctionalInterface
public interface RegisterKeys {
    void onRegisterKeys(KeyRegistry keyRegistry);
}
