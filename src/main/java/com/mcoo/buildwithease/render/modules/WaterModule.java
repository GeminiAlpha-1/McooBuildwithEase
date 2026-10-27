package com.mcoo.buildwithease.render.modules;

import java.util.List;

import com.google.auto.service.AutoService;

import com.mcoo.buildwithease.render.AbstractRenderModule;
import dev.huskuraft.universal.api.render.RenderModuleCategory;
import dev.huskuraft.universal.api.render.RenderParameter;

/**
 * 水 / 流体。
 */
@AutoService(dev.huskuraft.universal.api.render.RenderModule.class)
public class WaterModule extends AbstractRenderModule {

    public static final String ID = "water";

    public static final RenderParameter WAVE_HEIGHT = RenderParameter.ofFloat(
            paramKey(ID, "waveHeight"), paramKey(ID, "waveHeight"), 0.5f, 0f, 2f);
    public static final RenderParameter REFLECTION = RenderParameter.ofChoice(
            paramKey(ID, "reflection"), paramKey(ID, "reflection"), "sky", List.of("off", "sky", "ssr"));
    public static final RenderParameter MURKINESS = RenderParameter.ofFloat(
            paramKey(ID, "murkiness"), paramKey(ID, "murkiness"), 0.3f, 0f, 1f);
    public static final RenderParameter CAUSTICS = RenderParameter.ofBoolean(
            paramKey(ID, "caustics"), paramKey(ID, "caustics"), false);
    public static final RenderParameter UNDERWATER_FOG = RenderParameter.ofBoolean(
            paramKey(ID, "underwaterFog"), paramKey(ID, "underwaterFog"), true);

    public WaterModule() {
        super(ID, RenderModuleCategory.WATER);
    }

    @Override
    public List<RenderParameter> parameters() {
        return List.of(WAVE_HEIGHT, REFLECTION, MURKINESS, CAUSTICS, UNDERWATER_FOG);
    }

}
