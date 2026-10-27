package dev.huskuraft.universal.api.gui;

/**
 * ImGui 是否可用。
 * <p>
 * 由平台层在客户端初始化时设置：client 包 JiJ 打包了 ImGui 库时为 true，
 * 没装（或用 server 包）时为 false —— 这时二级 / 三级菜单直接隐藏，不崩。
 */
public final class ImGuiSupport {

    private static volatile boolean available = false;

    private ImGuiSupport() {
    }

    public static boolean isAvailable() {
        return available;
    }

    /** 平台层在客户端初始化时调用。 */
    public static void setAvailable(boolean value) {
        available = value;
    }

}
