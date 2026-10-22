package dev.huskuraft.universal.vanilla.sound;

import dev.huskuraft.universal.api.core.ResourceLocation;
import dev.huskuraft.universal.api.sound.Sound;
import dev.huskuraft.universal.vanilla.core.MinecraftResourceLocation;
import net.minecraft.sounds.SoundEvent;

public record MinecraftSound(
    SoundEvent refs
) implements Sound {

    @Override
    public ResourceLocation getId() {
        return new MinecraftResourceLocation(refs.location());
    }
}
