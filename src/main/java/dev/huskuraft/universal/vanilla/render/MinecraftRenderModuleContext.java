package dev.huskuraft.universal.vanilla.render;

import dev.huskuraft.universal.api.core.ResourceLocation;
import dev.huskuraft.universal.api.render.RenderModuleContext;
import dev.huskuraft.universal.api.render.RenderParameter;
import dev.huskuraft.universal.api.render.RenderParameterRegistry;
import dev.huskuraft.universal.api.render.RenderPipelineHandle;
import dev.huskuraft.universal.api.render.RenderPipelineSpec;

/**
 * 26.2 的渲染模块上下文。
 * <p>
 * 模块只能通过它拿参数、申请管线、写日志 —— 拿不到别的模块，也拿不到渲染器内部结构。
 */
public final class MinecraftRenderModuleContext implements RenderModuleContext {

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(MinecraftRenderModuleContext.class);

    private final String moduleId;
    private final RenderParameterRegistry parameters;

    public MinecraftRenderModuleContext(String moduleId, RenderParameterRegistry parameters) {
        this.moduleId = moduleId;
        this.parameters = parameters;
    }

    @Override
    public String moduleId() {
        return moduleId;
    }

    @Override
    public RenderParameterRegistry parameters() {
        return parameters;
    }

    // ---- 参数读取 ----

    @Override
    public boolean booleanParameter(RenderParameter parameter) {
        return Boolean.TRUE.equals(parameters.get(moduleId, parameter));
    }

    @Override
    public int intParameter(RenderParameter parameter) {
        var value = parameters.get(moduleId, parameter);
        return value instanceof Number number ? number.intValue() : 0;
    }

    @Override
    public float floatParameter(RenderParameter parameter) {
        var value = parameters.get(moduleId, parameter);
        return value instanceof Number number ? number.floatValue() : 0f;
    }

    @Override
    public String stringParameter(RenderParameter parameter) {
        var value = parameters.get(moduleId, parameter);
        return value == null ? "" : String.valueOf(value);
    }

    @Override
    public int colorParameter(RenderParameter parameter) {
        var value = parameters.get(moduleId, parameter);
        return value instanceof Number number ? number.intValue() : 0xFFFFFFFF;
    }

    // ---- 渲染能力 ----

    @Override
    public RenderPipelineHandle requestPipeline(RenderPipelineSpec spec) {
        return requestPipeline(spec, null);
    }

    @Override
    public RenderPipelineHandle requestPipeline(RenderPipelineSpec spec, ResourceLocation texture) {
        var renderType = MinecraftRenderPipelineFactory.create(spec, texture);
        return new MinecraftRenderPipelineHandle(spec.id(), renderType);
    }

    // ---- 日志 ----

    @Override
    public void log(String message) {
        LOGGER.info("[render/{}] {}", moduleId, message);
    }

    @Override
    public void log(String message, Throwable error) {
        LOGGER.error("[render/{}] {}", moduleId, message, error);
    }

}
