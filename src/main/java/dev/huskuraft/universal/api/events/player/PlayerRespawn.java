package dev.huskuraft.universal.api.events.player;

import dev.huskuraft.universal.api.core.Player;

@FunctionalInterface
public interface PlayerRespawn {
    void onPlayerRespawn(Player oldPlayer, Player newPlayer, boolean alive);
}
