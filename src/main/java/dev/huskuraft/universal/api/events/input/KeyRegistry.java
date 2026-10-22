package dev.huskuraft.universal.api.events.input;

import dev.huskuraft.universal.api.input.KeyBindingOwner;

public interface KeyRegistry {

    void register(KeyBindingOwner key);
}
