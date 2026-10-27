package com.mcoo.buildwithease.render.modules;

import java.util.List;

import com.google.auto.service.AutoService;

import com.mcoo.buildwithease.render.AbstractRenderModule;
import dev.huskuraft.universal.api.render.RenderModuleCategory;
import dev.huskuraft.universal.api.render.RenderParameter;

/**
 * 抗锯齿。
 */
@AutoService(dev.huskuraft.universal.api.render.RenderModule.class)
public class AntiAliasModule extends AbstractRenderModule {

    public static final String ID = "antialias";

    public static final RenderParameter FXAA = RenderParameter.ofBoolean(
            paramKey(ID, "fxaa"), paramKey(ID, "fxaa"), false);
    public static final RenderParameter TAA = RenderParameter.ofBoolean(
            paramKey(ID, "taa"), paramKey(ID, "taa"), false);
    public static final RenderParameter SHARPEN = RenderParameter.ofFloat(
            paramKey(ID, "sharpen"), paramKey(ID, "sharpen"), 0f, 0f, 1f);

    public AntiAliasModule() {
        super(ID, RenderModuleCategory.ANTIALIAS);
    }

    @Override
    public List<RenderParameter> parameters() {
        return List.of(FXAA, TAA, SHARPEN);
    }

}
