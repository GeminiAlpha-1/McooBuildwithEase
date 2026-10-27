package com.mcoo.buildwithease.render.modules;

import java.util.List;

import com.google.auto.service.AutoService;

import com.mcoo.buildwithease.render.AbstractRenderModule;
import dev.huskuraft.universal.api.render.RenderModuleCategory;
import dev.huskuraft.universal.api.render.RenderParameter;

/**
 * 画质档位 / 性能。
 * <p>
 * 档位是一个「总开关式」的参数：低档位时其它模块的耗能项会自动被压低。
 */
@AutoService(dev.huskuraft.universal.api.render.RenderModule.class)
public class QualityModule extends AbstractRenderModule {

    public static final String ID = "quality";

    public static final RenderParameter PRESET = RenderParameter.ofChoice(
            paramKey(ID, "preset"), paramKey(ID, "preset"), "medium", List.of("low", "medium", "high", "ultra"));
    public static final RenderParameter INTERNAL_RESOLUTION = RenderParameter.ofFloat(
            paramKey(ID, "internalResolution"), paramKey(ID, "internalResolution"), 1f, 0.5f, 2f);
    public static final RenderParameter RENDER_DISTANCE = RenderParameter.ofInt(
            paramKey(ID, "renderDistance"), paramKey(ID, "renderDistance"), 16, 2, 32);

    public QualityModule() {
        super(ID, RenderModuleCategory.QUALITY);
    }

    @Override
    public List<RenderParameter> parameters() {
        return List.of(PRESET, INTERNAL_RESOLUTION, RENDER_DISTANCE);
    }

}
