package dev.huskuraft.universal.fabric.platform;

import dev.huskuraft.universal.api.platform.Entrance;
import dev.huskuraft.universal.fabric.events.common.PlatformLifecycleEvents;
import net.fabricmc.api.ModInitializer;

public class FabricInitializer implements ModInitializer {

    @Override
    public void onInitialize() {
        Entrance.getInstance();

        PlatformLifecycleEvents.COMMON_START.invoker().onLaunch();
    }

}
