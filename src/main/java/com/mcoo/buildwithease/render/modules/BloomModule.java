package com.mcoo.buildwithease.render.modules;

import java.util.List;

import com.google.auto.service.AutoService;

import com.mcoo.buildwithease.render.AbstractRenderModule;
import dev.huskuraft.universal.api.render.RenderModuleCategory;
import dev.huskuraft.universal.api.render.RenderParameter;

/**
 * 辉光 / 泛光（bloom）。
 * <p>
 * 实现路径：26.2 的后处理链是数据驱动的 JSON（{@code assets/<命名空间>/post_effect/*.json}），
 * 照着 vanilla 的 {@code blur.json} 写一份 bloom 链：亮部提取 → box_blur(H) → box_blur(V) → 加色合成。
 */
@AutoService(dev.huskuraft.universal.api.render.RenderModule.class)
public class BloomModule extends AbstractRenderModule {

    public static final String ID = "bloom";

    public static final RenderParameter INTENSITY = RenderParameter.ofFloat(
            paramKey(ID, "intensity"), paramKey(ID, "intensity"), 0.5f, 0f, 2f);
    public static final RenderParameter RADIUS = RenderParameter.ofFloat(
            paramKey(ID, "radius"), paramKey(ID, "radius"), 2f, 0f, 10f);
    public static final RenderParameter THRESHOLD = RenderParameter.ofFloat(
            paramKey(ID, "threshold"), paramKey(ID, "threshold"), 0.8f, 0f, 1f);

    public BloomModule() {
        super(ID, RenderModuleCategory.BLOOM);
    }

    @Override
    public List<RenderParameter> parameters() {
        return List.of(INTENSITY, RADIUS, THRESHOLD);
    }

}
