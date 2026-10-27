package dev.huskuraft.universal.api.render;

/**
 * 渲染管线的句柄（不透明引用）。
 * <p>
 * 业务层只拿得到句柄，拿不到平台层的具体类型 —— 这样模块代码不会依赖 Minecraft 的类。
 */
public interface RenderPipelineHandle extends PlatformHandle {

    /** 管线 id。 */
    String id();

}
