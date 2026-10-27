package dev.huskuraft.universal.fabric.platform;

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

        ClientEntrance.getInstance();

        // 自建渲染管线要在第一次资源重载之前登记进 vanilla 的管线表，
        // 否则 ShaderManager 那一轮扫不到它们的着色器
        MinecraftCustomPipelines.ensureRegistered();

        PlatformLifecycleEvents.CLIENT_START.invoker().onLaunch();
        MinecraftSelfTest.runIfRequested();
    }

}
