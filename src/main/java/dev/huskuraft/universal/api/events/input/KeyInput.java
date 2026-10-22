package dev.huskuraft.universal.api.events.input;

import dev.huskuraft.universal.api.input.InputKey;

@FunctionalInterface
public interface KeyInput {
    void onKeyInput(InputKey key);
}
