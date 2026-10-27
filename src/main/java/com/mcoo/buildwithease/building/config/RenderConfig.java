package com.mcoo.buildwithease.building.config;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 渲染相关配置。
 * <p>
 * 除了老的「方块预览 / 显示别人的建造 / 最大渲染体积」之外，
 * 还有<b>自建渲染器</b>的三块状态：总开关、每个模块的开关、每个模块的参数值。
 * <p>
 * 参数值统一放在 {@link #parameters} 里（键 = {@code 模块id.参数key}），
 * 声明和校验都在渲染框架那边，这里只负责存取。
 */
public record RenderConfig(
        boolean showBlockPreview,
        boolean showOtherPlayersBuild,
        boolean showOtherPlayersBuildTooltips,
        int maxRenderVolume,
        int maxRenderDistance,
        boolean rendererEnabled,
        Map<String, Boolean> moduleEnabled,
        Map<String, Object> parameters
) {

    public static final int MAX_RENDER_VOLUME_DEFAULT = 4096;
    public static final int MAX_RENDER_VOLUME_MIN = 0;
    public static final int MAX_RENDER_VOLUME_MAX = 4096;

    /** 兼容旧调用点：只传原来的五个字段。 */
    public RenderConfig(
            boolean showBlockPreview,
            boolean showOtherPlayersBuild,
            boolean showOtherPlayersBuildTooltips,
            int maxRenderVolume,
            int maxRenderDistance
    ) {
        this(showBlockPreview, showOtherPlayersBuild, showOtherPlayersBuildTooltips, maxRenderVolume, maxRenderDistance,
                false, Map.of(), Map.of());
    }

    public RenderConfig() {
        this(true, true, false, MAX_RENDER_VOLUME_DEFAULT, 128);
    }

    public static RenderConfig DEFAULT = new RenderConfig();

    public RenderConfig withRendererEnabled(boolean rendererEnabled) {
        return new RenderConfig(showBlockPreview, showOtherPlayersBuild, showOtherPlayersBuildTooltips,
                maxRenderVolume, maxRenderDistance, rendererEnabled, moduleEnabled, parameters);
    }

    public RenderConfig withModuleEnabled(Map<String, Boolean> moduleEnabled) {
        return new RenderConfig(showBlockPreview, showOtherPlayersBuild, showOtherPlayersBuildTooltips,
                maxRenderVolume, maxRenderDistance, rendererEnabled, moduleEnabled, parameters);
    }

    public RenderConfig withParameters(Map<String, Object> parameters) {
        return new RenderConfig(showBlockPreview, showOtherPlayersBuild, showOtherPlayersBuildTooltips,
                maxRenderVolume, maxRenderDistance, rendererEnabled, moduleEnabled, parameters);
    }

    public Map<String, Boolean> moduleEnabled() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(moduleEnabled));
    }

    public Map<String, Object> parameters() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(parameters));
    }

}
