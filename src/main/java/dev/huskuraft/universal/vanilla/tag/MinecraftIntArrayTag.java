package dev.huskuraft.universal.vanilla.tag;

import dev.huskuraft.universal.api.tag.IntArrayTag;

public record MinecraftIntArrayTag(net.minecraft.nbt.IntArrayTag refs) implements IntArrayTag {

    @Override
    public byte getId() {
        return new MinecraftTag(refs).getId();
    }

    @Override
    public String getAsString() {
        return new MinecraftTag(refs).getAsString();
    }

    @Override
    public int[] getAsIntArray() {
        return refs.getAsIntArray();
    }
}
