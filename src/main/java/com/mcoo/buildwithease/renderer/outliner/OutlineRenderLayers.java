package com.mcoo.buildwithease.renderer.outliner;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dev.huskuraft.universal.api.core.ResourceLocation;
import dev.huskuraft.universal.api.renderer.RenderLayer;
import dev.huskuraft.universal.api.renderer.RenderLayers;
import dev.huskuraft.universal.api.renderer.VanillaLayer;

/**
 * 选区 / 描边用的渲染层。
 * <p>
 * 26.2 的 {@code RenderType} 没法自建（{@code RenderType.create} 是包内可见），
 * 所以这里挑的都是 vanilla 现成的管线。
 * <p>
 * <b>与上游 1.21.3 的差异</b>：上游用 {@code RenderStateShard} 自由组合出了
 * 「只在不重叠处绘制」（NOTEQUAL 深度测试）、「永远画在最上层」（NEVER 深度测试）、
 * 「加色混合」、「剔除/不剔除」这几种语义。26.2 的 vanilla 管线把这些都固定死了，
 * 因此这几处目前<b>退化</b>成同一条管线。真要恢复，需要自建 {@code RenderPipeline}
 * （见 PORTING-PLAN「自建管线」一节）。
 */
public abstract class OutlineRenderLayers extends RenderLayers {

    public static final ResourceLocation BLANK_TEXTURE_LOCATION = ResourceLocation.of("mcoo_build_with_ease", "textures/misc/blank.png");
    public static final ResourceLocation CHECKERED_TEXTURE_LOCATION = ResourceLocation.of("mcoo_build_with_ease", "textures/misc/checkerboard.png");
    public static final ResourceLocation CHECKERED_HIGHLIGHT_TEXTURE_LOCATION = ResourceLocation.of("mcoo_build_with_ease", "textures/misc/checkerboard_highlight.png");

    public static final ResourceLocation CHECKERED_THIN_TEXTURE_LOCATION = ResourceLocation.of("mcoo_build_with_ease", "textures/misc/checkerboard_thin_64.png");
    public static final ResourceLocation CHECKERED_CUTOUT_TEXTURE_LOCATION = ResourceLocation.of("mcoo_build_with_ease", "textures/misc/checkerboard_cutout.png");
    public static final ResourceLocation SELECTION_TEXTURE_LOCATION = ResourceLocation.of("mcoo_build_with_ease", "textures/misc/selection_64.png");

    /** 按贴图缓存的渲染层：同一张贴图永远拿到同一个渲染层，批次才不会串。 */
    private static final Map<ResourceLocation, RenderLayer> TRANSLUCENT_LAYERS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, RenderLayer> SOLID_LAYERS = new ConcurrentHashMap<>();

    /**
     * 选区实心层。
     * <p>
     * 上游这里有「重叠 / 不重叠」两套深度测试（NOTEQUAL / NEVER），26.2 的 vanilla 管线
     * 只有固定的深度测试，所以三个变体现在共用这一条。
     */
    protected static final RenderLayer OUTLINE_SOLID = RenderLayer.vanilla(VanillaLayer.ENTITY_SOLID, BLANK_TEXTURE_LOCATION);

    protected static final RenderLayer GLOWING_SOLID_DEFAULT = glowingSolid(RenderLayers.BLOCK_ATLAS_LOCATION);
    protected static final RenderLayer GLOWING_TRANSLUCENT_DEFAULT = glowingTranslucent(RenderLayers.BLOCK_ATLAS_LOCATION);
    protected static final RenderLayer ITEM_PARTIAL_SOLID = RenderLayer.vanilla(VanillaLayer.ENTITY_SOLID, RenderLayers.BLOCK_ATLAS_LOCATION);
    protected static final RenderLayer ITEM_PARTIAL_TRANSLUCENT = RenderLayer.vanilla(VanillaLayer.ENTITY_TRANSLUCENT, RenderLayers.BLOCK_ATLAS_LOCATION);
    protected static final RenderLayer FLUID = RenderLayer.vanilla(VanillaLayer.ENTITY_TRANSLUCENT, RenderLayers.BLOCK_ATLAS_LOCATION);
    protected static final RenderLayer ADDITIVE = RenderLayer.vanilla(VanillaLayer.TRANSLUCENT_BLOCK);

    public static RenderLayer outlineSolid() {
        return OUTLINE_SOLID;
    }

    /**
     * 选区实心层。
     *
     * @param overlap 上游靠深度测试区分「重叠 / 不重叠」，26.2 的 vanilla 管线没有这个口子，
     *                目前两个分支画出来是一样的
     */
    public static RenderLayer outlineSolid(boolean overlap) {
        return OUTLINE_SOLID;
    }

    /**
     * 选区半透明层（带贴图）。
     *
     * @param cull 上游靠剔除状态区分，26.2 的 vanilla 管线固定为剔除，暂时不起作用
     */
    public static RenderLayer outlineTranslucent(ResourceLocation texture, boolean cull) {
        return TRANSLUCENT_LAYERS.computeIfAbsent(texture,
                key -> RenderLayer.vanilla(VanillaLayer.ENTITY_TRANSLUCENT, key));
    }

    public static RenderLayer glowingSolid(ResourceLocation texture) {
        return SOLID_LAYERS.computeIfAbsent(texture,
                key -> RenderLayer.vanilla(VanillaLayer.ENTITY_SOLID, key));
    }

    public static RenderLayer glowingSolid() {
        return GLOWING_SOLID_DEFAULT;
    }

    public static RenderLayer glowingTranslucent(ResourceLocation texture) {
        return TRANSLUCENT_LAYERS.computeIfAbsent(texture,
                key -> RenderLayer.vanilla(VanillaLayer.ENTITY_TRANSLUCENT, key));
    }

    public static RenderLayer additive() {
        return ADDITIVE;
    }

    public static RenderLayer glowingTranslucent() {
        return GLOWING_TRANSLUCENT_DEFAULT;
    }

    public static RenderLayer itemPartialSolid() {
        return ITEM_PARTIAL_SOLID;
    }

    public static RenderLayer itemPartialTranslucent() {
        return ITEM_PARTIAL_TRANSLUCENT;
    }

    public static RenderLayer fluid() {
        return FLUID;
    }

}
