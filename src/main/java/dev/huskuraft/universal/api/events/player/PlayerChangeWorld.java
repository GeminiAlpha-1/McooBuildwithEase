package dev.huskuraft.universal.api.events.player;

import dev.huskuraft.universal.api.core.Player;
import dev.huskuraft.universal.api.core.World;

@FunctionalInterface
public interface PlayerChangeWorld {
    void onPlayerChangeWorld(Player player, World origin, World destination);
}
