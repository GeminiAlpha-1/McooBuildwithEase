package com.mcoo.buildwithease.render.modules;

import java.util.List;

import com.google.auto.service.AutoService;

import com.mcoo.buildwithease.render.AbstractRenderModule;
import dev.huskuraft.universal.api.render.RenderModuleCategory;
import dev.huskuraft.universal.api.render.RenderParameter;

/**
 * 云。
 */
@AutoService(dev.huskuraft.universal.api.render.RenderModule.class)
public class CloudsModule extends AbstractRenderModule {

    public static final String ID = "clouds";

    public static final RenderParameter MODE = RenderParameter.ofChoice(
            paramKey(ID, "mode"), paramKey(ID, "mode"), "blocky", List.of("blocky", "volumetric", "off"));
    public static final RenderParameter DENSITY = RenderParameter.ofFloat(
            paramKey(ID, "density"), paramKey(ID, "density"), 0.5f, 0f, 1f);
    public static final RenderParameter SPEED = RenderParameter.ofFloat(
            paramKey(ID, "speed"), paramKey(ID, "speed"), 1f, 0f, 4f);
    public static final RenderParameter COLOR = RenderParameter.ofColor(
            paramKey(ID, "color"), paramKey(ID, "color"), 0xFFFFFFFF);
    public static final RenderParameter SHADOWS = RenderParameter.ofBoolean(
            paramKey(ID, "shadows"), paramKey(ID, "shadows"), true);

    public CloudsModule() {
        super(ID, RenderModuleCategory.CLOUDS);
    }

    @Override
    public List<RenderParameter> parameters() {
        return List.of(MODE, DENSITY, SPEED, COLOR, SHADOWS);
    }

}
