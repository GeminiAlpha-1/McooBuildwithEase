package dev.huskuraft.universal.api.core;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import dev.huskuraft.universal.api.platform.PlatformReference;

public interface PlayerProfile extends PlatformReference {

    UUID getId();

    String getName();

    Map<String, ? extends Collection<?>> getProperties();

}
