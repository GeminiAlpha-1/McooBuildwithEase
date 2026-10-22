package dev.huskuraft.universal.vanilla.texture;

import com.google.auto.service.AutoService;

import dev.huskuraft.universal.api.texture.SimpleTexture;
import dev.huskuraft.universal.api.texture.SimpleTextureSprite;
import dev.huskuraft.universal.api.texture.SpriteScaling;
import dev.huskuraft.universal.api.texture.Texture;
import dev.huskuraft.universal.api.texture.TextureFactory;
import dev.huskuraft.universal.api.texture.TextureSprite;
import dev.huskuraft.universal.vanilla.core.MinecraftResourceLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;

@AutoService(TextureFactory.class)
public final class MinecraftTextureFactory implements TextureFactory {

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(MinecraftTextureFactory.class);

    /**
     * 26.2 的 GUI 精灵统一放在 GUI 图集里：
     * 图集纹理 = {@code textures/atlas/gui.png}，精灵定义目录 = {@code textures/gui/sprites}。
     * {@code SpriteId} 的第一个分量在不同版本里分别表示过这两个位置，
     * 所以这里两种都试一遍，取第一个能查到的。
     */
    private static final Identifier GUI_SPRITES_DIRECTORY = Identifier.withDefaultNamespace("textures/gui/sprites");

    @Override
    public Texture getBlockAtlasTexture() {
        // 26.2：方块图集位置在 TextureAtlas 上（1.21.3 是 InventoryMenu.BLOCK_ATLAS）
        return new SimpleTexture(new MinecraftResourceLocation(TextureAtlas.LOCATION_BLOCKS));
    }

    @Override
    public TextureSprite getBackgroundTextureSprite() {
        return null;
    }

    @Override
    public TextureSprite getButtonTextureSprite(boolean enabled, boolean focused) {
        // AbstractButton.SPRITES 在 26.2 是 private static，靠 class tweaker 打开
        return createTextureSprite(AbstractButton.SPRITES.get(enabled, focused));
    }

    @Override
    public TextureSprite getDemoBackgroundTextureSprite() {
        return createTextureSprite(Identifier.withDefaultNamespace("textures/gui/demo_background.png"), null, 248, 166, 0, 0, 256, 256, new SpriteScaling.NineSlice(248, 166, 6));
    }

    public TextureSprite createTextureSprite(Identifier texture, Identifier name, int width, int height, int x, int y, int textureWidth, int textureHeight, SpriteScaling scaling) {
        return new SimpleTextureSprite(MinecraftResourceLocation.ofNullable(texture), MinecraftResourceLocation.ofNullable(name), width, height, x, y, textureWidth, textureHeight, scaling);
    }

    /**
     * 26.2 的 GUI 精灵查找：{@link net.minecraft.client.resources.model.sprite.AtlasManager} + {@link SpriteId}。
     * <p>
     * 【26.2 修正】原实现只按图集纹理位置（{@code textures/atlas/gui.png}）查，
     * 查不到时 26.2 会返回「缺失精灵」→ 画出来就是紫色棋盘格 + 各种条纹。
     * 现在两种位置都试，并且把结果打进 debug 日志，便于核对。
     */
    public TextureSprite createTextureSprite(Identifier name) {
        var atlasManager = Minecraft.getInstance().getAtlasManager();
        TextureAtlasSprite sprite = null;
        try {
            sprite = atlasManager.get(new SpriteId(Sheets.GUI_SHEET, name));
        } catch (Throwable ignored) {
        }
        if (sprite == null) {
            try {
                sprite = atlasManager.get(new SpriteId(GUI_SPRITES_DIRECTORY, name));
            } catch (Throwable ignored) {
            }
        }
        if (sprite == null) {
            LOGGER.warn("GUI sprite not found: {}", name);
            return createTextureSprite(Sheets.GUI_SHEET, name, 16, 16, 0, 0, 256, 256, new SpriteScaling.Stretch());
        }
        return createTextureSprite(sprite);
    }

    public TextureSprite createTextureSprite(TextureAtlasSprite sprite) {
        return new SimpleTextureSprite(
                new MinecraftResourceLocation(Sheets.GUI_SHEET),
                new MinecraftResourceLocation(sprite.contents().name()),
                sprite.contents().width(),
                sprite.contents().height(),
                sprite.getX(),
                sprite.getY(),
                sprite.getU0(),
                sprite.getU1(),
                sprite.getV0(),
                sprite.getV1(),
                getSpriteScaling(sprite)
        );
    }

    /**
     * 26.2 里 GUI 精灵的缩放方式（拉伸 / 平铺 / 九宫格）存放在精灵的元数据里，
     * 但对应的 MetadataSectionType 没有对外暴露，取不到就退回 Stretch
     * （vanilla 的按钮精灵本身就是一张矩形，Stretch 画出来是对的）。
     */
    public SpriteScaling getSpriteScaling(TextureAtlasSprite sprite) {
        return new SpriteScaling.Stretch();
    }

}
