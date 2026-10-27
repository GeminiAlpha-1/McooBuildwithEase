package com.mcoo.buildwithease.render.modules;

import java.util.List;

import com.google.auto.service.AutoService;

import com.mcoo.buildwithease.render.AbstractRenderModule;
import dev.huskuraft.universal.api.render.RenderModuleCategory;
import dev.huskuraft.universal.api.render.RenderParameter;

/**
 * 体积光 / 上帝光（god rays）。
 */
@AutoService(dev.huskuraft.universal.api.render.RenderModule.class)
public class GodRaysModule extends AbstractRenderModule {

    public static final String ID = "godrays";

    public static final RenderParameter INTENSITY = RenderParameter.ofFloat(
            paramKey(ID, "intensity"), paramKey(ID, "intensity"), 0.5f, 0f, 2f);
    public static final RenderParameter QUALITY = RenderParameter.ofChoice(
            paramKey(ID, "quality"), paramKey(ID, "quality"), "medium", List.of("low", "medium", "high"));
    public static final RenderParameter COLOR = RenderParameter.ofColor(
            paramKey(ID, "color"), paramKey(ID, "color"), 0xFFFFD9A0);

    public GodRaysModule() {
        super(ID, RenderModuleCategory.GODRAYS);
    }

    @Override
    public List<RenderParameter> parameters() {
        return List.of(INTENSITY, QUALITY, COLOR);
    }

}
