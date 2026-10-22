package dev.huskuraft.universal.api.events.player;

import dev.huskuraft.universal.api.core.Player;

@FunctionalInterface
public interface PlayerLoggedIn {
    void onPlayerLoggedIn(Player player);
}
