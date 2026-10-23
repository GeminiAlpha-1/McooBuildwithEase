package dev.huskuraft.universal.vanilla.tag;

import dev.huskuraft.universal.api.tag.LongArrayTag;

public record MinecraftLongArrayTag(net.minecraft.nbt.LongArrayTag refs) implements LongArrayTag {

    @Override
    public byte getId() {
        return new MinecraftTag(refs).getId();
    }

    @Override
    public String getAsString() {
        return new MinecraftTag(refs).getAsString();
    }

    @Override
    public long[] getAsLongArray() {
        return refs.getAsLongArray();
    }
}
