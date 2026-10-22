package com.mcoo.buildwithease;

import com.mcoo.buildwithease.screen.builer.McooBuildwithEaseBuilderSettingsScreen;
import com.mcoo.buildwithease.screen.clipboard.McooBuildwithEaseClipboardScreen;
import com.mcoo.buildwithease.screen.settings.McooBuildwithEaseSettingsScreen;
import com.mcoo.buildwithease.screen.structure.McooBuildwithEaseStructureScreen;

/**
 * 调试辅助：用一个系统属性让客户端自动打开指定界面，方便无人值守地截图核对 GUI。
 * <p>
 * 用法：{@code -Dmcoo.debugScreen=settings|builder|clipboard}。
 * 正常游戏不会设置这个属性，因此不产生任何影响。
 */
final class McooBuildwithEaseDebug {

    private static final String PROPERTY = "mcoo.debugScreen";
    private static boolean opened = false;

    private McooBuildwithEaseDebug() {
    }

    static void openScreenIfRequested(McooBuildwithEaseClientManager manager) {
        if (opened) {
            return;
        }
        var screen = System.getProperty(PROPERTY);
        if (screen == null || screen.isBlank()) {
            return;
        }
        opened = true;
        try {
            var entrance = McooBuildwithEaseClient.getInstance();
            switch (screen) {
                case "settings" -> new McooBuildwithEaseSettingsScreen(entrance).attach();
                case "builder" -> new McooBuildwithEaseBuilderSettingsScreen(entrance).attach();
                case "clipboard" -> new McooBuildwithEaseClipboardScreen(entrance).attach();
                case "radial" -> new McooBuildwithEaseStructureScreen(entrance, McooBuildwithEaseKeys.BUILD_MODE_RADIAL.getKeyBinding()).attach();
                default -> McooBuildwithEase.LOGGER.warn("Unknown debug screen: {}", screen);
            }
        } catch (Throwable throwable) {
            McooBuildwithEase.LOGGER.error("Failed to open debug screen {}", screen, throwable);
        }
    }

}
