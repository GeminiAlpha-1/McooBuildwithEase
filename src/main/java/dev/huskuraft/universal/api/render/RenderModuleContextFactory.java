package dev.huskuraft.universal.api.render;

/**
 * 渲染模块上下文的工厂（由平台层实现并注册）。
 * <p>
 * API 层不知道 Minecraft 的存在，所以「怎么建管线、怎么写日志」这类事
 * 交给平台层 —— 它是 API 层与 Minecraft 之间唯一的接缝。
 */
public interface RenderModuleContextFactory {

    /** 全局唯一的工厂（平台层在初始化时设进去）。 */
    static RenderModuleContextFactory get() {
        return Holder.INSTANCE;
    }

    /** 平台层初始化时调用。 */
    static void set(RenderModuleContextFactory factory) {
        Holder.INSTANCE = factory;
    }

    /** 给某个模块创建上下文。 */
    RenderModuleContext create(String moduleId, RenderParameterRegistry parameters);

    final class Holder {
        private static volatile RenderModuleContextFactory INSTANCE;

        private Holder() {
        }
    }

}
