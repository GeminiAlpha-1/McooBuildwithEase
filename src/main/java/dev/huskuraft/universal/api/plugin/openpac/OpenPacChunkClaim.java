package dev.huskuraft.universal.api.plugin.openpac;

import dev.huskuraft.universal.api.platform.PlatformReference;

import javax.annotation.Nonnull;

import java.util.UUID;

public interface OpenPacChunkClaim extends PlatformReference {

    @Nonnull
    UUID getPlayerId();

}
