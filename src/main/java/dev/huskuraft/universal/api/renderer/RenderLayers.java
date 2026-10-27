package dev.huskuraft.universal.api.renderer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dev.huskuraft.universal.api.core.ResourceLocation;
import dev.huskuraft.universal.api.texture.TextureFactory;

/**
 * 常用的渲染层。
 * <p>
 * 上游 1.21.3 这里是「{@code RenderStateShard} 各分量的自由组合」，26.2 里 vanilla 的
 * {@code RenderType} 把这些语义全部固定死了，所以这里只保留「挑一条现成管线」的能力。
 */
public abstract class RenderLayers {

    public static final ResourceLocation BLOCK_ATLAS_LOCATION = TextureFactory.getInstance().getBlockAtlasTexture().resource();

    /** GUI 矩形 / 四边形（纯顶点色）。 */
    public static final RenderLayer GUI = RenderLayer.vanilla(VanillaLayer.DEBUG_FILLED_BOX);
    /** GUI 渐变遮罩（列表头尾的阴影）。 */
    public static final RenderLayer GUI_OVERLAY = RenderLayer.vanilla(VanillaLayer.DEBUG_FILLED_BOX);
    /** 输入框文本选区高亮。 */
    public static final RenderLayer GUI_TEXT_HIGHLIGHT = RenderLayer.vanilla(VanillaLayer.DEBUG_FILLED_BOX);

    /**
     * 带贴图的 GUI 矩形。
     * <p>
     * 按贴图缓存：每张贴图一条渲染层，这样同一帧里画多张贴图时不会挤进同一批
     * （批次是按渲染层分组的，挤在一起就只有第一张贴图）。
     */
    private static final Map<ResourceLocation, RenderLayer> TEXTURED_LAYERS = new ConcurrentHashMap<>();

    /**
     * 带贴图的 GUI 矩形。
     *
     * @param blur   26.2 的 vanilla {@code RenderType} 采样方式固定，此参数不再生效
     * @param mipmap 同上
     */
    public static RenderLayer texture(ResourceLocation location, boolean blur, boolean mipmap) {
        return TEXTURED_LAYERS.computeIfAbsent(location, key -> RenderLayer.vanilla(VanillaLayer.TEXT, key));
    }

    /** 带贴图、带顶点色的 GUI 矩形。 */
    public static RenderLayer colorTexture(ResourceLocation location, boolean blur, boolean mipmap) {
        return TEXTURED_LAYERS.computeIfAbsent(location, key -> RenderLayer.vanilla(VanillaLayer.TEXT, key));
    }

}
