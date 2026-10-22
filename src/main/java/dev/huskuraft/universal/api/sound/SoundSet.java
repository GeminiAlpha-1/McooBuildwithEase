package dev.huskuraft.universal.api.sound;

import dev.huskuraft.universal.api.platform.PlatformReference;

public interface SoundSet extends PlatformReference {

    float volume();

    float pitch();

    Sound breakSound();

    Sound stepSound();

    Sound placeSound();

    Sound hitSound();

    Sound fallSound();

}
