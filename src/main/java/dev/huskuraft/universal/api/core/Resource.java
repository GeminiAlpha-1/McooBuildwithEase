package dev.huskuraft.universal.api.core;

import java.io.IOException;

import dev.huskuraft.universal.api.platform.PlatformReference;

public interface Resource extends PlatformReference {

//    ResourceLocation location();

    ResourceMetadata metadata() throws IOException;

}
