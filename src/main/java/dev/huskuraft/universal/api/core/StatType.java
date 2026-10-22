package dev.huskuraft.universal.api.core;

import dev.huskuraft.universal.api.platform.PlatformReference;

public interface StatType<T extends PlatformReference> extends PlatformReference {

    Stat<T> get(T value);

}
