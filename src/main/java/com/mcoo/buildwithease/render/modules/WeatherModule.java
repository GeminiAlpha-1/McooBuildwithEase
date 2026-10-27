package com.mcoo.buildwithease.render.modules;

import java.util.List;

import com.google.auto.service.AutoService;

import com.mcoo.buildwithease.render.AbstractRenderModule;
import dev.huskuraft.universal.api.render.RenderModuleCategory;
import dev.huskuraft.universal.api.render.RenderParameter;

/**
 * 天气 / 雾。
 */
@AutoService(dev.huskuraft.universal.api.render.RenderModule.class)
public class WeatherModule extends AbstractRenderModule {

    public static final String ID = "weather";

    public static final RenderParameter RAIN_INTENSITY = RenderParameter.ofFloat(
            paramKey(ID, "rainIntensity"), paramKey(ID, "rainIntensity"), 1f, 0f, 2f);
    public static final RenderParameter FOG_DENSITY = RenderParameter.ofFloat(
            paramKey(ID, "fogDensity"), paramKey(ID, "fogDensity"), 0.2f, 0f, 1f);
    public static final RenderParameter WETNESS = RenderParameter.ofBoolean(
            paramKey(ID, "wetness"), paramKey(ID, "wetness"), false);

    public WeatherModule() {
        super(ID, RenderModuleCategory.WEATHER);
    }

    @Override
    public List<RenderParameter> parameters() {
        return List.of(RAIN_INTENSITY, FOG_DENSITY, WETNESS);
    }

}
