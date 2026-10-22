package dev.huskuraft.universal.api.core;

import java.util.UUID;

import dev.huskuraft.universal.api.platform.PlatformReference;
import dev.huskuraft.universal.api.text.Text;

public interface PlayerInfo extends PlatformReference {

    PlayerProfile getProfile();

    Text getDisplayName();

    PlayerSkin getSkin();

    default UUID getId() {
        return getProfile().getId();
    }

    default String getName() {
        return getProfile().getName();
    }

}
