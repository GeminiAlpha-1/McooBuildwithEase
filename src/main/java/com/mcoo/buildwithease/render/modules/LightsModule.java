package com.mcoo.buildwithease.render.modules;

import java.util.List;

import com.google.auto.service.AutoService;

import com.mcoo.buildwithease.render.AbstractRenderModule;
import dev.huskuraft.universal.api.render.RenderModuleCategory;
import dev.huskuraft.universal.api.render.RenderParameter;

/**
 * 自定义光源（放置各种光源）。
 * <p>
 * 与「光照」模块的区别：光照改的是<b>全局</b>的光照风格，这里管的是
 * <b>玩家自己摆的</b>点光源（颜色 / 强度 / 半径 / 数量上限 / 能不能摆）。
 */
@AutoService(dev.huskuraft.universal.api.render.RenderModule.class)
public class LightsModule extends AbstractRenderModule {

    public static final String ID = "lights";

    public static final RenderParameter ENABLED = RenderParameter.ofBoolean(
            paramKey(ID, "enabled"), paramKey(ID, "enabled"), false);
    public static final RenderParameter COLOR = RenderParameter.ofColor(
            paramKey(ID, "color"), paramKey(ID, "color"), 0xFFFFCC66);
    public static final RenderParameter INTENSITY = RenderParameter.ofFloat(
            paramKey(ID, "intensity"), paramKey(ID, "intensity"), 1f, 0f, 4f);
    public static final RenderParameter RADIUS = RenderParameter.ofFloat(
            paramKey(ID, "radius"), paramKey(ID, "radius"), 8f, 0f, 32f);
    public static final RenderParameter MAX_COUNT = RenderParameter.ofInt(
            paramKey(ID, "maxCount"), paramKey(ID, "maxCount"), 16, 0, 64);
    public static final RenderParameter PLACEABLE = RenderParameter.ofBoolean(
            paramKey(ID, "placeable"), paramKey(ID, "placeable"), true);

    public LightsModule() {
        super(ID, RenderModuleCategory.LIGHTS);
    }

    @Override
    public List<RenderParameter> parameters() {
        return List.of(ENABLED, COLOR, INTENSITY, RADIUS, MAX_COUNT, PLACEABLE);
    }

}
