package dev.huskuraft.universal.api.sound;

import dev.huskuraft.universal.api.core.ResourceLocation;
import dev.huskuraft.universal.api.platform.PlatformLoader;

public interface SoundFactory {

    static SoundFactory getInstance() {
        return PlatformLoader.getSingleton();
    }

    SoundInstance createSimpleSoundInstance(ResourceLocation location,
                                            SoundSource source,
                                            float volume,
                                            float pitch,
                                            boolean looping,
                                            int delay,
                                            SoundInstance.Attenuation attenuation,
                                            double x,
                                            double y,
                                            double z,
                                            boolean relative);

}
