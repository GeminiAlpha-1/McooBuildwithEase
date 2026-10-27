package com.mcoo.buildwithease.render.modules;

import java.util.List;

import com.google.auto.service.AutoService;

import com.mcoo.buildwithease.render.AbstractRenderModule;
import dev.huskuraft.universal.api.render.RenderModuleCategory;
import dev.huskuraft.universal.api.render.RenderParameter;

/**
 * 色调映射 / 色彩分级。
 */
@AutoService(dev.huskuraft.universal.api.render.RenderModule.class)
public class ToneMapModule extends AbstractRenderModule {

    public static final String ID = "tonemap";

    public static final RenderParameter EXPOSURE = RenderParameter.ofFloat(
            paramKey(ID, "exposure"), paramKey(ID, "exposure"), 1f, 0f, 2f);
    public static final RenderParameter CONTRAST = RenderParameter.ofFloat(
            paramKey(ID, "contrast"), paramKey(ID, "contrast"), 1f, 0f, 2f);
    public static final RenderParameter SATURATION = RenderParameter.ofFloat(
            paramKey(ID, "saturation"), paramKey(ID, "saturation"), 1f, 0f, 2f);
    public static final RenderParameter TEMPERATURE = RenderParameter.ofFloat(
            paramKey(ID, "temperature"), paramKey(ID, "temperature"), 0f, -1f, 1f);
    public static final RenderParameter VIGNETTE = RenderParameter.ofFloat(
            paramKey(ID, "vignette"), paramKey(ID, "vignette"), 0.2f, 0f, 1f);
    public static final RenderParameter ACES = RenderParameter.ofBoolean(
            paramKey(ID, "aces"), paramKey(ID, "aces"), false);

    public ToneMapModule() {
        super(ID, RenderModuleCategory.TONEMAP);
    }

    @Override
    public List<RenderParameter> parameters() {
        return List.of(EXPOSURE, CONTRAST, SATURATION, TEMPERATURE, VIGNETTE, ACES);
    }

}
