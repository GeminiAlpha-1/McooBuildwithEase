package com.mcoo.buildwithease.renderer.opertaion;

import dev.huskuraft.universal.api.renderer.RenderLayer;
import dev.huskuraft.universal.api.renderer.VanillaLayer;

/**
 * 建造相关的渲染层。
 * <p>
 * 26.2 的 {@code RenderType} 没法自建（{@code RenderType.create} 是包内可见），
 * 所以这里挑的都是 vanilla 现成的管线。
 */
public abstract class BlockRenderLayers {

    /**
     * 变换器的半透明面（纯顶点色）。
     * <p>
     * 上游 1.21.3 用的是「视图 Z 偏移 + 半透明 + 不写深度」的自定义合成层，
     * 26.2 的 vanilla 管线没有这些自由组合的口子，这里退到纯色填充管线。
     */
    public static final RenderLayer EF_PLANES = RenderLayer.vanilla(VanillaLayer.DEBUG_FILLED_BOX);

    /** 调试线框（半透明）。 */
    public static final RenderLayer EF_LINES = RenderLayer.vanilla(VanillaLayer.LINES_TRANSLUCENT);

    public static RenderLayer lines() {
        return EF_LINES;
    }

    public static RenderLayer planes() {
        return EF_PLANES;
    }

}
