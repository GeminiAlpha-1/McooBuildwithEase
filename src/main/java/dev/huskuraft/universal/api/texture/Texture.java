package dev.huskuraft.universal.api.texture;

import java.util.Set;

import dev.huskuraft.universal.api.core.ResourceLocation;

public interface Texture {

    ResourceLocation resource();

    Set<ResourceLocation> sprites();

    TextureSprite getSprite(ResourceLocation name);

}
