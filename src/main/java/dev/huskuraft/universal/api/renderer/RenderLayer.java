package dev.huskuraft.universal.api.renderer;

import dev.huskuraft.universal.api.core.ResourceLocation;
import dev.huskuraft.universal.api.platform.PlatformReference;

/**
 * 渲染层。
 * <p>
 * 26.2 起渲染层只能是 <b>vanilla 现成的 {@code RenderType}</b>（{@code RenderType.create} 是包内可见，
 * 外部无法自建），所以这里只有一种构造方式：挑一条 {@link VanillaLayer}，由平台层翻译成
 * 具体的 {@code RenderType}。
 * <p>
 * 上游 1.21.3 那一套「{@code RenderStateShard} 各分量自由组合」的接口在 26.2 已经无法还原 ——
 * vanilla 的 {@code RenderType} 把深度测试、剔除、混合、输出目标都固定死了。
 * 与其留一个「设了也不生效」的假接口，不如把选择写成枚举：意图明确，行为可预期。
 */
public interface RenderLayer extends PlatformReference {

    /** vanilla 现成渲染层（不需要贴图）。 */
    static RenderLayer vanilla(VanillaLayer layer) {
        return RenderLayerFactory.getInstance().createVanillaRenderLayer(layer, null);
    }

    /**
     * vanilla 现成渲染层（带贴图）。
     *
     * @param texture 贴图位置；只有 {@link VanillaLayer#ENTITY_SOLID} /
     *                 {@link VanillaLayer#ENTITY_TRANSLUCENT} / {@link VanillaLayer#TEXT} 用得上
     */
    static RenderLayer vanilla(VanillaLayer layer, ResourceLocation texture) {
        return RenderLayerFactory.getInstance().createVanillaRenderLayer(layer, texture);
    }

    /**
     * 本模组自建的渲染层（带贴图）。
     * <p>
     * 自建管线能表达 vanilla {@code RenderType} 表达不了的语义
     * （深度测试 / 加色混合 / 剔除），见 {@link CustomLayer}。
     */
    static RenderLayer custom(CustomLayer layer, ResourceLocation texture) {
        return RenderLayerFactory.getInstance().createCustomRenderLayer(layer, texture);
    }

}
