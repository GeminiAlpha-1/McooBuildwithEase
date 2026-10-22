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
        // 26.2 里 vanilla 已经删掉了 "textures/gui/demo_background.png"（上游 1.21.3 用的就是这张），
        // 直接沿用旧路径会画成洋红色的「缺失纹理」。
        // 这里改用本 mod 自己的 32x32 面板底图，按九宫格（边框 6px）画 —— 和上游的画法一致：
        // 浅灰面板 + 2px 黑边 + 2px 白高光，深色标题字（0x404040）能看清。
        return createTextureSprite(Identifier.fromNamespaceAndPath("mcoo_build_with_ease", "textures/gui/panel_background.png"),
                null, 32, 32, 0, 0, 32, 32, new SpriteScaling.NineSlice(32, 32, 6));
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
     * <p>
     * 【26.2 修正 · 精灵 id】返回的 {@link TextureSprite} 里 {@code name} 必须存
     * <b>原始的精灵 id</b>（例如 {@code minecraft:widget/button}）：
     * {@code GuiGraphicsExtractor#blitSprite} 是拿这个 id 去 GUI 图集里查精灵的，
     * 换成图集里的真实名字就查不到了。
     */
    public TextureSprite createTextureSprite(Identifier name) {
        var atlasManager = Minecraft.getInstance().getAtlasManager();
        TextureAtlasSprite sprite = null;
        var how = "";
        try {
            // 路径 1：按图集纹理位置查（Sheets.GUI_SHEET = textures/atlas/gui.png）
            var atlas = atlasManager.getAtlasOrThrow(Sheets.GUI_SHEET);
            sprite = atlas.getSprite(name);
            how = "atlas-by-texture";
        } catch (Throwable throwable) {
            LOGGER.debug("GUI atlas lookup by texture failed for {}: {}", name, throwable.toString());
        }
        if (sprite == null) {
            try {
                // 路径 2：按精灵定义目录查（textures/gui/sprites）
                sprite = atlasManager.get(new SpriteId(GUI_SPRITES_DIRECTORY, name));
                how = "sprite-id-by-directory";
            } catch (Throwable throwable) {
                LOGGER.debug("GUI sprite lookup by directory failed for {}: {}", name, throwable.toString());
            }
        }
        if (sprite == null) {
            try {
                // 路径 3：按图集纹理位置 + SpriteId
                sprite = atlasManager.get(new SpriteId(Sheets.GUI_SHEET, name));
                how = "sprite-id-by-texture";
            } catch (Throwable throwable) {
                LOGGER.debug("GUI sprite lookup by texture id failed for {}: {}", name, throwable.toString());
            }
        }
        if (sprite == null) {
            LOGGER.warn("GUI sprite not found: {}", name);
            // name 留空：这样渲染层会退回 UV 拉伸路径，而不是拿一个不存在的 id 去 blitSprite
            return createTextureSprite(Sheets.GUI_SHEET, null, 16, 16, 0, 0, 256, 256, new SpriteScaling.Stretch());
        }
        LOGGER.info("GUI sprite {} found via {}: atlas={} u=({}..{}) v=({}..{}) size={}x{}",
                name, how, sprite.atlasLocation(), sprite.getU0(), sprite.getU1(), sprite.getV0(), sprite.getV1(),
                sprite.contents().width(), sprite.contents().height());
        return createTextureSprite(sprite, name);
    }

    public TextureSprite createTextureSprite(TextureAtlasSprite sprite) {
        return createTextureSprite(sprite, sprite.contents().name());
    }

    /**
     * @param id 精灵 id，写进 {@link TextureSprite#name()}，供 {@code blitSprite} 查图集用
     */
    public TextureSprite createTextureSprite(TextureAtlasSprite sprite, Identifier id) {
        return new SimpleTextureSprite(
                // 26.2 的 TextureAtlasSprite 自带 atlasLocation()，用它比写死 GUI_SHEET 可靠
                new MinecraftResourceLocation(sprite.atlasLocation()),
                new MinecraftResourceLocation(id),
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
