package com.mcoo.buildwithease.render.modules;

import java.util.List;

import com.google.auto.service.AutoService;

import com.mcoo.buildwithease.render.AbstractRenderModule;
import dev.huskuraft.universal.api.render.RenderModuleCategory;
import dev.huskuraft.universal.api.render.RenderParameter;

/**
 * 天空 / 太阳 / 月亮。
 * <p>
 * 参数对应现代光影包的「太阳角度 / 时间 / 月相 / 星空」那一组。
 * <b>实现留空</b>：接口与参数先占好，底层效果按模块逐个填。
 */
@AutoService(dev.huskuraft.universal.api.render.RenderModule.class)
public class SkyModule extends AbstractRenderModule {

    public static final String ID = "sky";

    public static final RenderParameter SUN_ANGLE = RenderParameter.ofFloat(
            paramKey(ID, "sunAngle"), paramKey(ID, "sunAngle"), 0f, 0f, 360f);
    public static final RenderParameter TIME_OFFSET = RenderParameter.ofInt(
            paramKey(ID, "timeOffset"), paramKey(ID, "timeOffset"), 0, -24000, 24000);
    public static final RenderParameter FREEZE_TIME = RenderParameter.ofBoolean(
            paramKey(ID, "freezeTime"), paramKey(ID, "freezeTime"), false);
    public static final RenderParameter MOON_PHASE = RenderParameter.ofInt(
            paramKey(ID, "moonPhase"), paramKey(ID, "moonPhase"), 0, 0, 7);
    public static final RenderParameter STAR_INTENSITY = RenderParameter.ofFloat(
            paramKey(ID, "starIntensity"), paramKey(ID, "starIntensity"), 1f, 0f, 2f);
    public static final RenderParameter SKY_BRIGHTNESS = RenderParameter.ofFloat(
            paramKey(ID, "skyBrightness"), paramKey(ID, "skyBrightness"), 1f, 0f, 2f);

    public SkyModule() {
        super(ID, RenderModuleCategory.SKY);
    }

    @Override
    public List<RenderParameter> parameters() {
        return List.of(SUN_ANGLE, TIME_OFFSET, FREEZE_TIME, MOON_PHASE, STAR_INTENSITY, SKY_BRIGHTNESS);
    }

}
