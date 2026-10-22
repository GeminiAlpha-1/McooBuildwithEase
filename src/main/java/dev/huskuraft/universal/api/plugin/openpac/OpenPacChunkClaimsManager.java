package dev.huskuraft.universal.api.plugin.openpac;

import dev.huskuraft.universal.api.core.BlockPosition;
import dev.huskuraft.universal.api.core.ChunkPosition;
import dev.huskuraft.universal.api.core.ResourceLocation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface OpenPacChunkClaimsManager {

    @Nullable
    OpenPacChunkClaim get(@Nonnull ResourceLocation dimension, int x, int z);

    @Nullable
    OpenPacChunkClaim get(@Nonnull ResourceLocation dimension, @Nonnull ChunkPosition chunkPosition);

    @Nullable
    OpenPacChunkClaim get(@Nonnull ResourceLocation dimension, @Nonnull BlockPosition blockPosition);
}
