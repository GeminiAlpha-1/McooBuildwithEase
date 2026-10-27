package com.mcoo.buildwithease.render;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mcoo.buildwithease.building.config.RenderConfig;
import dev.huskuraft.universal.api.render.RenderModule;
import dev.huskuraft.universal.api.render.RenderModuleHost;
import dev.huskuraft.universal.api.render.RenderParameterRegistry;

/**
 * 自建渲染器的总管。
 * <p>
 * 职责：拥有 {@link RenderModuleHost}，把模块发现、参数持久化、每帧调度串起来。
 * <p>
 * <b>隔离</b>：所有异常都在宿主里被兜住 —— 任何一个模块出问题，
 * 只会禁用那一个模块，游戏照常跑（见 {@link RenderModuleHost}）。
 */
public class RenderManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(RenderManager.class);

    private final RenderModuleHost host = new RenderModuleHost((message, error) -> {
        if (error != null) {
            LOGGER.error(message, error);
        } else {
            LOGGER.info(message);
        }
    });

    /** 初始化：发现模块 → 登记参数 → 逐个注册。 */
    public void initialize() {
        host.discover();
        host.registerAll();
        LOGGER.info("[render] 已加载 {} 个渲染模块：{}", host.modules().size(), host.modules().stream()
                .map(RenderModule::id).toList());
    }

    /** 从配置恢复：总开关、模块开关、参数值。 */
    public void applyConfig(RenderConfig config) {
        host.setMasterEnabled(config.rendererEnabled());
        config.moduleEnabled().forEach(host::setModuleEnabled);
        host.parameters().restore(config.parameters());
    }

    /** 把当前状态写回配置。 */
    public RenderConfig saveTo(RenderConfig config) {
        var moduleEnabled = new java.util.LinkedHashMap<String, Boolean>();
        for (var module : host.modules()) {
            moduleEnabled.put(module.id(), host.isModuleEnabled(module.id()));
        }
        return config
                .withRendererEnabled(host.isMasterEnabled())
                .withModuleEnabled(moduleEnabled)
                .withParameters(host.parameters().snapshot());
    }

    public void tick(float deltaTick) {
        host.tick(deltaTick);
    }

    public void renderWorld(float deltaTick) {
        host.renderWorld(deltaTick);
    }

    public void renderGui(float deltaTick) {
        host.renderGui(deltaTick);
    }

    public RenderModuleHost host() {
        return host;
    }

    public RenderParameterRegistry parameters() {
        return host.parameters();
    }

    /** 当前失败的模块 → 异常（界面上可以提示玩家去关掉它）。 */
    public Map<String, Throwable> failures() {
        return host.failures();
    }

}
