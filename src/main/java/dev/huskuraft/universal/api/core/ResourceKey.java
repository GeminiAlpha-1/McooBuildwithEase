package dev.huskuraft.universal.api.core;

import dev.huskuraft.universal.api.platform.PlatformReference;

public interface ResourceKey<T> extends PlatformReference {

    ResourceLocation registry();

    ResourceLocation location();

}
