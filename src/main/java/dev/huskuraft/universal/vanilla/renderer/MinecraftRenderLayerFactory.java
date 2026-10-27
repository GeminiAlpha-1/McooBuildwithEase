package dev.huskuraft.universal.vanilla.renderer;

import com.google.auto.service.AutoService;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;

import dev.huskuraft.universal.api.core.ResourceLocation;
import dev.huskuraft.universal.api.renderer.CustomLayer;
import dev.huskuraft.universal.api.renderer.RenderLayer;
import dev.huskuraft.universal.api.renderer.RenderLayerFactory;
import dev.huskuraft.universal.api.renderer.VanillaLayer;
import dev.huskuraft.universal.api.renderer.VertexFormat;
import dev.huskuraft.universal.api.renderer.VertexFormats;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/**
 * 26.2 的渲染层实现。
 * <p>
 * <b>关键限制</b>：26.2 的 {@code RenderType} 构造器是 private，静态工厂
 * {@code RenderType.create(String, RenderSetup)} 是包内可见 —— 外部（也就是本工程）
 * <b>无法自建 {@code RenderType}</b>，只能复用 {@link RenderTypes} 里的现成条目。
 * <p>
 * 所以这里就是一张「{@link VanillaLayer} → vanilla {@code RenderType}」的对照表。
 * 想换一条管线，先在 {@link VanillaLayer} 里加一项，再在这里给出对应实现。
 */
@AutoService(RenderLayerFactory.class)
public final class MinecraftRenderLayerFactory implements RenderLayerFactory {

    @Override
    public RenderLayer createVanillaRenderLayer(VanillaLayer layer, ResourceLocation texture) {
        var id = toIdentifier(texture);
        var renderType = switch (layer) {
            case LINES -> RenderTypes.LINES;
            case LINES_TRANSLUCENT -> RenderTypes.LINES_TRANSLUCENT;
            case DEBUG_FILLED_BOX -> RenderTypes.debugFilledBox();
            case SOLID_BLOCK -> RenderTypes.solidMovingBlock();
            case TRANSLUCENT_BLOCK -> RenderTypes.translucentMovingBlock();
            case ENTITY_SOLID -> RenderTypes.entitySolid(requireTexture(layer, id));
            case ENTITY_TRANSLUCENT -> RenderTypes.entityTranslucent(requireTexture(layer, id));
            case TEXT -> RenderTypes.text(requireTexture(layer, id));
        };
        return new MinecraftRenderLayer(layer.name(), renderType, id);
    }

    /** 需要贴图的管线不能没有贴图 —— 与其传一个 null 让 vanilla 后面炸掉，不如在这里说清楚。 */
    private static Identifier requireTexture(VanillaLayer layer, Identifier texture) {
        if (texture == null) {
            throw new IllegalArgumentException("渲染层 " + layer + " 需要贴图");
        }
        return texture;
    }

    @Override
    public RenderLayer createCustomRenderLayer(CustomLayer layer, ResourceLocation texture) {
        var id = toIdentifier(texture);
        var pipeline = switch (layer) {
            case OUTLINE_SHELL -> MinecraftCustomPipelines.OUTLINE_SHELL;
            case OUTLINE_OVERLAY -> MinecraftCustomPipelines.OUTLINE_OVERLAY;
            case ADDITIVE -> MinecraftCustomPipelines.ADDITIVE;
        };
        // 配方照抄 vanilla 的实体管线：Sampler0 贴图 + 光照图 + overlay
        var builder = RenderSetup.builder(pipeline).useLightmap().useOverlay();
        if (id != null) {
            builder.withTexture("Sampler0", id);
        }
        var setup = builder.createRenderSetup();
        return new MinecraftRenderLayer(layer.name(), RenderType.create(layer.name(), setup), id);
    }

    @Override
    public VertexFormat getVertexFormat(VertexFormats formats) {
        var format = switch (formats) {
            // 26.2 没有 BLIT_SCREEN，用 POSITION_TEX 顶替
            case BLIT_SCREEN -> DefaultVertexFormat.POSITION_TEX;
            case BLOCK -> DefaultVertexFormat.BLOCK;
            // 26.2 把 NEW_ENTITY 改名为 ENTITY
            case NEW_ENTITY -> DefaultVertexFormat.ENTITY;
            case PARTICLE -> DefaultVertexFormat.PARTICLE;
            case POSITION -> DefaultVertexFormat.POSITION;
            case POSITION_COLOR -> DefaultVertexFormat.POSITION_COLOR;
            case POSITION_COLOR_NORMAL -> DefaultVertexFormat.POSITION_COLOR_NORMAL;
            case POSITION_COLOR_LIGHTMAP -> DefaultVertexFormat.POSITION_COLOR_LIGHTMAP;
            case POSITION_TEX -> DefaultVertexFormat.POSITION_TEX;
            // 26.2 没有 POSITION_COLOR_TEX，用 POSITION_COLOR_TEX_LIGHTMAP 顶替
            case POSITION_COLOR_TEX -> DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP;
            case POSITION_TEX_COLOR -> DefaultVertexFormat.POSITION_TEX_COLOR;
            case POSITION_COLOR_TEX_LIGHTMAP -> DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP;
            case POSITION_TEX_LIGHTMAP_COLOR -> DefaultVertexFormat.POSITION_TEX_LIGHTMAP_COLOR;
            case POSITION_TEX_COLOR_NORMAL -> DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL;
        };
        return () -> format;
    }

    @Override
    public VertexFormat.Mode getVertexFormatMode(VertexFormats.Modes modes) {
        // 26.2 用 com.mojang.blaze3d.PrimitiveTopology 取代了 VertexFormat.Mode
        var mode = switch (modes) {
            case LINES -> PrimitiveTopology.LINES;
            case LINE_STRIP -> PrimitiveTopology.DEBUG_LINE_STRIP;
            case DEBUG_LINES -> PrimitiveTopology.DEBUG_LINES;
            case DEBUG_LINE_STRIP -> PrimitiveTopology.DEBUG_LINE_STRIP;
            case TRIANGLES -> PrimitiveTopology.TRIANGLES;
            case TRIANGLE_STRIP -> PrimitiveTopology.TRIANGLE_STRIP;
            case TRIANGLE_FAN -> PrimitiveTopology.TRIANGLE_FAN;
            case QUADS -> PrimitiveTopology.QUADS;
        };
        return () -> mode;
    }

    private static Identifier toIdentifier(ResourceLocation location) {
        if (location == null) {
            return null;
        }
        return Identifier.fromNamespaceAndPath(location.getNamespace(), location.getPath());
    }

}
