package dev.huskuraft.universal.api.renderer;

import dev.huskuraft.universal.api.core.ResourceLocation;
import dev.huskuraft.universal.api.platform.PlatformLoader;

/**
 * 渲染层的平台工厂。
 * <p>
 * 业务层只认 {@link VanillaLayer} 这个抽象，具体落到哪条 vanilla 管线由平台实现决定
 * （本工程只有 Fabric / MC 26.2 一个实现：{@code MinecraftRenderLayerFactory}）。
 */
public interface RenderLayerFactory {

    static RenderLayerFactory getInstance() {
        return PlatformLoader.getSingleton();
    }

    /**
     * 创建一条渲染层。
     *
     * @param layer   要用的 vanilla 管线
     * @param texture 贴图位置（可空）
     */
    RenderLayer createVanillaRenderLayer(VanillaLayer layer, ResourceLocation texture);

    /**
     * 创建一条自建渲染层。
     *
     * @param layer   自建管线
     * @param texture 贴图位置（可空）
     */
    RenderLayer createCustomRenderLayer(CustomLayer layer, ResourceLocation texture);

    VertexFormat getVertexFormat(VertexFormats formats);

    VertexFormat.Mode getVertexFormatMode(VertexFormats.Modes modes);

}
