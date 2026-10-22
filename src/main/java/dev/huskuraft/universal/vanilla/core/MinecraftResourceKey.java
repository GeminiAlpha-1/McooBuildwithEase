package dev.huskuraft.universal.vanilla.core;

import dev.huskuraft.universal.api.core.ResourceKey;
import dev.huskuraft.universal.api.core.ResourceLocation;

public record MinecraftResourceKey<T>(
        net.minecraft.resources.ResourceKey<?> refs
) implements ResourceKey<T> {

    @Override
    public ResourceLocation registry() {
        return new MinecraftResourceLocation(refs.registry());
    }

    @Override
    public ResourceLocation location() {
        // 26.2：ResourceKey#location() 改名为 identifier()
        return new MinecraftResourceLocation(refs.identifier());
    }

}
