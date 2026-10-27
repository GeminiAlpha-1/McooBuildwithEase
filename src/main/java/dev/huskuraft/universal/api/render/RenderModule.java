package dev.huskuraft.universal.api.render;

import java.util.List;

/**
 * 渲染模块的 SPI（服务提供者接口）。
 * <p>
 * <b>一个模块 = 一个独立可关的功能</b>。模块之间互不引用、互不知道对方存在，
 * 需要什么能力就向 {@link RenderModuleContext} 要。
 * <p>
 * <b>隔离契约（宿主保证，模块可以放心依赖）</b>：
 * <ol>
 *     <li>模块的任何一个生命周期回调抛异常 → <b>只禁用这一个模块</b>，
 *         其余模块和游戏本体完全不受影响；</li>
 *     <li>模块可以在配置里单独关闭；</li>
 *     <li>模块的 {@link #onRegister} 失败不会影响其它模块的注册；</li>
 *     <li>模块抛异常时会被记录到 {@link RenderModuleHost#failedModules()}，
 *         界面上能看出「哪个模块挂了、因为什么」。</li>
 * </ol>
 * <p>
 * 实现类用 {@code @AutoService(RenderModule.class)} 标注，宿主启动时自动发现。
 */
public interface RenderModule {

    /** 模块 id，全局唯一；也是配置里开关的键、参数键的前缀。 */
    String id();

    /** 分类（给 UI 分组）。 */
    RenderModuleCategory category();

    /** 显示名的语言键，默认 {@code render.module.<id>}。 */
    default String nameKey() {
        return "render.module." + id();
    }

    /** 这个模块声明的参数。模块自己声明，宿主统一收集。 */
    default List<RenderParameter> parameters() {
        return List.of();
    }

    /**
     * 注册（模块启用时调用一次）。
     * <p>
     * 在这里申请渲染管线、注册事件、读配置。
     * 抛异常 → 该模块被禁用。
     */
    default void onRegister(RenderModuleContext context) {
    }

    /** 注销（模块被关闭 / 客户端退出时调用）。 */
    default void onUnregister(RenderModuleContext context) {
    }

    /** 客户端每 tick。 */
    default void onClientTick(RenderModuleContext context, float deltaTick) {
    }

    /** 世界渲染阶段（每帧）。 */
    default void onRenderWorld(RenderModuleContext context, float deltaTick) {
    }

    /** GUI 渲染阶段（每帧）。 */
    default void onRenderGui(RenderModuleContext context, float deltaTick) {
    }

}
