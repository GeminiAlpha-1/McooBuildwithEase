package com.mcoo.buildwithease.render.modules;

import java.util.List;

import com.google.auto.service.AutoService;

import com.mcoo.buildwithease.render.AbstractRenderModule;
import dev.huskuraft.universal.api.render.RenderModuleCategory;
import dev.huskuraft.universal.api.render.RenderParameter;

/**
 * 植被摆动 / 粒子。
 */
@AutoService(dev.huskuraft.universal.api.render.RenderModule.class)
public class FoliageModule extends AbstractRenderModule {

    public static final String ID = "foliage";

    public static final RenderParameter SWAY_STRENGTH = RenderParameter.ofFloat(
            paramKey(ID, "swayStrength"), paramKey(ID, "swayStrength"), 1f, 0f, 2f);
    public static final RenderParameter PARTICLE_DENSITY = RenderParameter.ofFloat(
            paramKey(ID, "particleDensity"), paramKey(ID, "particleDensity"), 1f, 0f, 2f);

    public FoliageModule() {
        super(ID, RenderModuleCategory.FOLIAGE);
    }

    @Override
    public List<RenderParameter> parameters() {
        return List.of(SWAY_STRENGTH, PARTICLE_DENSITY);
    }

}
