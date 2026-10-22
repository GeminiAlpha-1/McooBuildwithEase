package dev.huskuraft.universal.api.plugin.ftbchunks;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import dev.huskuraft.universal.api.core.BlockPosition;
import dev.huskuraft.universal.api.core.World;

public interface FtbChunkClaimsManager {

    @Nullable
    FtbClaimedChunk get(@Nonnull World world, @Nonnull BlockPosition blockPosition);

}
