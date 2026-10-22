package dev.huskuraft.universal.vanilla.core;

import dev.huskuraft.universal.api.core.ResourceLocation;

/**
 * 26.2 起 Minecraft 的资源标识类叫 {@code net.minecraft.resources.Identifier}（不再是 ResourceLocation）。
 * 本类实现的是 common-api 的 {@link ResourceLocation}（接口名保持上游原样，不要改名，
 * 否则会和 Minecraft 自己的 Identifier 撞名）。
 */
public record MinecraftResourceLocation(
        net.minecraft.resources.Identifier refs
) implements ResourceLocation {

    public static ResourceLocation ofNullable(net.minecraft.resources.Identifier refs) {
        if (refs == null) return null;
        return new MinecraftResourceLocation(refs);
    }

    @Override
    public String getNamespace() {
        return refs.getNamespace();
    }

    @Override
    public String getPath() {
        return refs.getPath();
    }

    @Override
    public String toString() {
        return getNamespace() + ":" + getPath();
    }
}
