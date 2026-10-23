package dev.huskuraft.universal.fabric.platform;

import dev.huskuraft.universal.api.platform.ClientEntrance;
import dev.huskuraft.universal.fabric.events.common.PlatformLifecycleEvents;
import dev.huskuraft.universal.vanilla.MinecraftSelfTest;
import net.fabricmc.api.ClientModInitializer;

public class FabricClientInitializer implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientEntrance.getInstance();

        PlatformLifecycleEvents.CLIENT_START.invoker().onLaunch();
        MinecraftSelfTest.runIfRequested();
    }

}
