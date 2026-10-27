package com.mcoo.buildwithease.render.modules;

import java.util.List;

import com.google.auto.service.AutoService;

import com.mcoo.buildwithease.render.AbstractRenderModule;
import dev.huskuraft.universal.api.render.RenderModuleCategory;
import dev.huskuraft.universal.api.render.RenderParameter;

/**
 * 光照 / 阴影。
 * <p>
 * 参数对应光影包的「阴影分辨率 / 阴影距离 / 柔和阴影 / 光照风格 / 环境光强度」那一组。
 */
@AutoService(dev.huskuraft.universal.api.render.RenderModule.class)
public class LightingModule extends AbstractRenderModule {

    public static final String ID = "lighting";

    public static final RenderParameter STYLE = RenderParameter.ofChoice(
            paramKey(ID, "style"), paramKey(ID, "style"), "balanced", List.of("survival", "balanced", "builder"));
    public static final RenderParameter AMBIENT_INTENSITY = RenderParameter.ofFloat(
            paramKey(ID, "ambientIntensity"), paramKey(ID, "ambientIntensity"), 1f, 0f, 2f);
    public static final RenderParameter BLOCK_LIGHT_INTENSITY = RenderParameter.ofFloat(
            paramKey(ID, "blockLightIntensity"), paramKey(ID, "blockLightIntensity"), 1f, 0f, 2f);
    public static final RenderParameter SHADOWS = RenderParameter.ofBoolean(
            paramKey(ID, "shadows"), paramKey(ID, "shadows"), true);
    public static final RenderParameter SHADOW_RESOLUTION = RenderParameter.ofInt(
            paramKey(ID, "shadowResolution"), paramKey(ID, "shadowResolution"), 2048, 512, 8192);
    public static final RenderParameter SHADOW_DISTANCE = RenderParameter.ofInt(
            paramKey(ID, "shadowDistance"), paramKey(ID, "shadowDistance"), 16, 1, 32);
    public static final RenderParameter SOFT_SHADOWS = RenderParameter.ofBoolean(
            paramKey(ID, "softShadows"), paramKey(ID, "softShadows"), true);
    public static final RenderParameter CONTACT_SHADOWS = RenderParameter.ofBoolean(
            paramKey(ID, "contactShadows"), paramKey(ID, "contactShadows"), true);

    public LightingModule() {
        super(ID, RenderModuleCategory.LIGHTING);
    }

    @Override
    public List<RenderParameter> parameters() {
        return List.of(STYLE, AMBIENT_INTENSITY, BLOCK_LIGHT_INTENSITY, SHADOWS,
                SHADOW_RESOLUTION, SHADOW_DISTANCE, SOFT_SHADOWS, CONTACT_SHADOWS);
    }

}
