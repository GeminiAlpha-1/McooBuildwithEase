package dev.huskuraft.universal.vanilla.sound;

import dev.huskuraft.universal.api.core.BlockPosition;
import dev.huskuraft.universal.api.core.BlockState;
import dev.huskuraft.universal.api.core.Direction;
import dev.huskuraft.universal.api.platform.ParticleEngine;
import dev.huskuraft.universal.vanilla.core.MinecraftConvertor;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;

public record MinecraftParticleEngine(
        net.minecraft.client.particle.ParticleEngine refs
) implements ParticleEngine {

    /**
     * 26.2 的 ParticleEngine 上没有 {@code destroy(pos, state)} / {@code crack(pos, dir)} 了
     * （方块破坏粒子改由方块渲染状态驱动）。
     * 这里改为直接往关卡里丢方块粒子，保持「破坏方块时有碎屑」的表现。
     */
    @Override
    public void destroy(BlockPosition blockPosition, BlockState blockState) {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        var pos = MinecraftConvertor.toPlatformBlockPosition(blockPosition);
        var options = new BlockParticleOption(ParticleTypes.BLOCK, blockState.reference());
        level.addParticle(options, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0.0, 0.0, 0.0);
    }

    /**
     * 26.2 里没有独立的「裂纹粒子」，方块裂纹由渲染层处理 → 这里降级为空实现。
     */
    @Override
    public void crack(BlockPosition blockPosition, Direction direction) {
    }

}
