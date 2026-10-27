package dev.huskuraft.universal.api.render;

/**
 * 渲染模块能看到的「世界」。
 * <p>
 * <b>这是模块的能力边界</b>：模块只能通过它取参数、申请管线、提交几何、写日志 ——
 * 拿不到别的模块、拿不到渲染器的内部结构。
 * 这样任何一个模块的实现细节变化都不会波及别的模块。
 */
public interface RenderModuleContext {

    /** 当前模块的 id。 */
    String moduleId();

    /** 参数表（只能读到自己模块声明过的参数）。 */
    RenderParameterRegistry parameters();

    // ---- 参数读取（读的都是「当前模块」的参数） ----

    boolean booleanParameter(RenderParameter parameter);

    int intParameter(RenderParameter parameter);

    float floatParameter(RenderParameter parameter);

    String stringParameter(RenderParameter parameter);

    int colorParameter(RenderParameter parameter);

    // ---- 渲染能力 ----

    /**
     * 申请一条渲染管线。
     * <p>
     * 同一个 {@code spec.id()} 重复申请会拿到同一条管线（内部有缓存），
     * 所以模块可以在 {@link RenderModule#onRegister} 里申请一次，之后随便用。
     *
     * @return 管线的句柄；申请失败会抛异常 → 模块被禁用
     */
    RenderPipelineHandle requestPipeline(RenderPipelineSpec spec);

    /**
     * 申请一条带贴图的渲染管线。
     *
     * @param texture 贴图位置（实体 / 方块着色器必需）
     */
    RenderPipelineHandle requestPipeline(RenderPipelineSpec spec, dev.huskuraft.universal.api.core.ResourceLocation texture);

    /** 模块自己的日志。 */
    void log(String message);

    /** 模块自己的日志（带异常）。 */
    void log(String message, Throwable error);

}
