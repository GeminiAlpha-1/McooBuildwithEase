package dev.huskuraft.universal.api.render;

/**
 * 平台层对象的通用引用接口。
 * <p>
 * API 层不允许出现 Minecraft 的类型，但平台层的实现需要把具体对象传出去 ——
 * 这个接口就是那条「只在此处泄漏具体类型」的通道。
 */
public interface PlatformHandle {

    /** 平台层的具体对象。 */
    Object refs();

}
