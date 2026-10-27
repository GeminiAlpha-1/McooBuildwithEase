package dev.huskuraft.universal.fabric.platform;

import dev.huskuraft.universal.api.gui.ImGuiSupport;
import dev.huskuraft.universal.api.platform.ClientEntrance;
import dev.huskuraft.universal.api.render.RenderModuleContextFactory;
import dev.huskuraft.universal.fabric.events.common.PlatformLifecycleEvents;
import dev.huskuraft.universal.vanilla.MinecraftSelfTest;
import dev.huskuraft.universal.vanilla.renderer.MinecraftCustomPipelines;
import dev.huskuraft.universal.vanilla.render.MinecraftRenderModuleContext;
import net.fabricmc.api.ClientModInitializer;

public class FabricClientInitializer implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // 渲染模块的上下文工厂要先就位 —— 客户端入口创建时就会发现并注册渲染模块
        RenderModuleContextFactory.set((moduleId, parameters) -> new MinecraftRenderModuleContext(moduleId, parameters));

        // ImGui 在不在？不在的话二级 / 三级菜单直接隐藏（不崩）
        ImGuiSupport.setAvailable(isImGuiPresent());

        ClientEntrance.getInstance();

        // 自建渲染管线要在第一次资源重载之前登记进 vanilla 的管线表，
        // 否则 ShaderManager 那一轮扫不到它们的着色器
        MinecraftCustomPipelines.ensureRegistered();

        PlatformLifecycleEvents.CLIENT_START.invoker().onLaunch();
        MinecraftSelfTest.runIfRequested();
    }

    /**
     * ImGui 宿主（fabric-gui-imgui）是否可用。
     * <p>
     * 它的类被并进了我们的 jar（做法参照 Axiom），所以判断方式不是「有没有装那个 mod」，
     * 而是「类在不在」—— 被裁剪过的构建也能正确降级。
     */
    private static boolean isImGuiPresent() {
        try {
            Class.forName("cn.enaium.fabric.imgui.FabricImGui");
            return true;
        } catch (Throwable error) {
            return false;
        }
    }

}
