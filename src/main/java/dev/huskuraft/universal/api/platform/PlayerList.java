package dev.huskuraft.universal.api.platform;

import java.util.List;
import java.util.UUID;

import dev.huskuraft.universal.api.core.Player;
import dev.huskuraft.universal.api.core.PlayerProfile;

public interface PlayerList extends PlatformReference {

    List<Player> getPlayers();

    Player getPlayer(UUID uuid);

    boolean isOperator(PlayerProfile profile);

}
