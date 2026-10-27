package dev.huskuraft.universal.fabric.platform;

import dev.huskuraft.universal.api.platform.ClientEntrance;
import dev.huskuraft.universal.fabric.events.common.PlatformLifecycleEvents;
import dev.huskuraft.universal.vanilla.MinecraftSelfTest;
import dev.huskuraft.universal.vanilla.renderer.MinecraftCustomPipelines;
import net.fabricmc.api.ClientModInitializer;

public class FabricClientInitializer implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientEntrance.getInstance();

        // 自建渲染管线要在第一次资源重载之前登记进 vanilla 的管线表，
        // 否则 ShaderManager 那一轮扫不到它们的着色器
        MinecraftCustomPipelines.ensureRegistered();

        PlatformLifecycleEvents.CLIENT_START.invoker().onLaunch();
        MinecraftSelfTest.runIfRequested();
    }

}
