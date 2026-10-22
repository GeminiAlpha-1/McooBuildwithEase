package dev.huskuraft.universal.api.platform;

import dev.huskuraft.universal.api.core.BlockPosition;
import dev.huskuraft.universal.api.core.BlockState;
import dev.huskuraft.universal.api.core.Direction;

public interface ParticleEngine extends PlatformReference {

    void destroy(BlockPosition blockPosition, BlockState blockState);

    void crack(BlockPosition blockPosition, Direction direction);
}
