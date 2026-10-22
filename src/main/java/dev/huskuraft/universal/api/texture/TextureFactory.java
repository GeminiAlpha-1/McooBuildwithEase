package dev.huskuraft.universal.api.texture;

import dev.huskuraft.universal.api.platform.PlatformLoader;

public interface TextureFactory {

    static TextureFactory getInstance() {
        return PlatformLoader.getSingleton();
    }

    Texture getBlockAtlasTexture();

    TextureSprite getBackgroundTextureSprite();

    TextureSprite getButtonTextureSprite(boolean enabled, boolean focused);

    TextureSprite getDemoBackgroundTextureSprite();

}
