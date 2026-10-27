package dev.huskuraft.universal.vanilla.gui;

import cn.enaium.fabric.imgui.TextureBindings;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;

/**
 * 把 Minecraft 的纹理交给 ImGui 用（<b>纹理喂入的唯一落脚点</b>）。
 *
 * <h3>原理</h3>
 * ImGui 不认识 MC 的纹理对象，它只要一个 <b>long 纹理 id</b>
 * （{@code ImGui.image(long textureId, ...)} 的第一个参数）。
 * 宿主 {@code fabric-gui-imgui} 提供了从 {@code GpuTextureView} 到 id 的映射：
 * <pre>
 * cn.enaium.fabric.imgui.TextureBindings.INSTANCE.textureId(GpuTextureView) → long
 * </pre>
 * 宿主每帧在 {@code newFrame()} 里调 {@code TextureBindings.clearStale()}，
 * 所以 id 只保证"当帧有效"——<b>每帧现取</b>，不要缓存 id 跨帧使用
 * （内部按 view 身份缓存，重复调用是廉价的）。
 *
 * <h3>为什么取图集而不是逐个加载贴图</h3>
 * 方块/物品图标都在 {@code LOCATION_BLOCKS} 图集里，一次注册整张图集，
 * 图标只要按 sprite 的 UV 裁剪即可 —— 不会因为图标数量多而反复上传纹理。
 *
 * <p>⚠️ 本类引用了 ImGui 库与 MC 客户端类，只能从客户端入口路径到达。
 */
public final class McooImGuiTextures {

    private McooImGuiTextures() {
    }

    /**
     * 方块图集在 ImGui 里的纹理 id。
     *
     * @return 纹理 id；{@code 0} 表示拿不到（图集还没加载完 / 已释放）
     */
    public static long blockAtlasId() {
        var view = blockAtlasView();
        if (view == null) {
            return 0L;
        }
        return TextureBindings.INSTANCE.textureId(view);
    }

    /**
     * 方块图集里某个 sprite 的 UV 矩形。
     *
     * @param spritePath 形如 {@code "minecraft:block/stone"}（图集内的 sprite 名，不是贴图文件路径）
     * @return {@code {u0, v0, u1, v1}}；拿不到返回 {@code null}
     */
    public static float[] blockSpriteUv(String spritePath) {
        var atlas = blockAtlas();
        if (atlas == null) {
            return null;
        }
        var identifier = parse(spritePath);
        if (identifier == null) {
            return null;
        }
        var sprite = atlas.getSprite(identifier);
        if (sprite == null) {
            return null;
        }
        return new float[]{sprite.getU0(), sprite.getV0(), sprite.getU1(), sprite.getV1()};
    }

    /** 图集是否可用于绘制（图集在资源重载期间会短暂失效）。 */
    public static boolean isBlockAtlasReady() {
        return blockAtlasView() != null;
    }

    /**
     * 取方块图集。
     * <p>
     * ⚠️ **不要用 `Minecraft#getAtlasManager().getAtlasOrThrow(TextureAtlas.LOCATION_BLOCKS)`** ——
     * 实测抛 {@code IllegalArgumentException: Invalid atlas id: minecraft:textures/atlas/blocks.png}：
     * 26.2 的 {@link net.minecraft.client.resources.model.sprite.AtlasManager} 是按
     * **图集定义 id**（如 {@code minecraft:blocks}）索引的，不是按纹理路径。
     * 而 {@link net.minecraft.client.renderer.texture.TextureManager#getTexture}
     * 用纹理路径就能拿到 `TextureAtlas` 实例（它本身就是 `AbstractTexture` 子类）。
     * <p>
     * 两条路都留着：先走 TextureManager（实测可行），再退 AtlasManager。
     */
    private static TextureAtlas blockAtlas() {
        var minecraft = Minecraft.getInstance();
        try {
            var texture = minecraft.getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS);
            if (texture instanceof TextureAtlas atlas) {
                return atlas;
            }
        } catch (Throwable error) {
            // 资源重载窗口期拿不到是正常现象，继续走下面的兜底
            lastError = "TextureManager: " + error.getClass().getSimpleName() + ": " + error.getMessage();
        }
        try {
            return minecraft.getAtlasManager().getAtlasOrThrow(TextureAtlas.LOCATION_BLOCKS);
        } catch (Throwable error) {
            // 静默返回 null 会让"纹理没显示"变成无从排查的问题（踩过），所以留痕
            lastError = "AtlasManager: " + error.getClass().getSimpleName() + ": " + error.getMessage();
            return null;
        }
    }

    private static volatile String lastError;

    /**
     * 诊断信息：图集取不到时，界面/日志里需要有话说。
     * <p>
     * 返回多行文本，说明每个环节（AtlasManager / TextureManager / TextureView）的结果。
     */
    public static String diagnostics() {
        var lines = new StringBuilder();
        var minecraft = Minecraft.getInstance();
        lines.append("atlas id = ").append(TextureAtlas.LOCATION_BLOCKS).append('\n');

        TextureAtlas atlas = null;
        try {
            atlas = minecraft.getAtlasManager().getAtlasOrThrow(TextureAtlas.LOCATION_BLOCKS);
            lines.append("AtlasManager.getAtlasOrThrow -> ok\n");
        } catch (Throwable error) {
            lines.append("AtlasManager.getAtlasOrThrow -> ").append(error.getClass().getSimpleName())
                    .append(": ").append(error.getMessage()).append('\n');
        }

        if (atlas == null) {
            try {
                var fromTextureManager = minecraft.getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS);
                lines.append("TextureManager.getTexture -> ")
                        .append(fromTextureManager == null ? "null" : fromTextureManager.getClass().getSimpleName())
                        .append('\n');
                if (fromTextureManager instanceof TextureAtlas textureAtlas) {
                    atlas = textureAtlas;
                }
            } catch (Throwable error) {
                lines.append("TextureManager.getTexture -> ").append(error.getClass().getSimpleName())
                        .append(": ").append(error.getMessage()).append('\n');
            }
        }

        if (atlas != null) {
            var view = atlas.getTextureView();
            lines.append("atlas.getTextureView() -> ").append(view == null ? "null" : view.toString()).append('\n');
            lines.append("atlas sprite count sample: ")
                    .append(atlas.getSprite(parse("minecraft:block/stone")) == null ? "getSprite=null" : "getSprite=ok")
                    .append('\n');
        }
        if (lastError != null) {
            lines.append("last error = ").append(lastError).append('\n');
        }
        return lines.toString();
    }

    private static GpuTextureView blockAtlasView() {
        var atlas = blockAtlas();
        if (atlas == null) {
            return null;
        }
        return atlas.getTextureView();
    }

    /**
     * {@code "ns:path"} → {@link Identifier}。
     * <p>
     * 26.2 里 {@code ResourceLocation} 已改名 {@link Identifier}，构造入口是
     * {@code Identifier.fromNamespaceAndPath(ns, path)}（没有单参数的解析器，所以要自己切）。
     */
    private static Identifier parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        int colon = value.indexOf(':');
        if (colon < 0) {
            return null;
        }
        String namespace = value.substring(0, colon);
        String path = value.substring(colon + 1);
        if (namespace.isBlank() || path.isBlank()) {
            return null;
        }
        return Identifier.fromNamespaceAndPath(namespace, path);
    }

}
