package dev.huskuraft.universal.vanilla.tag;

import dev.huskuraft.universal.api.tag.ByteArrayTag;

public record MinecraftByteArrayTag(net.minecraft.nbt.ByteArrayTag refs) implements ByteArrayTag {

    @Override
    public byte getId() {
        return new MinecraftTag(refs).getId();
    }

    @Override
    public String getAsString() {
        return new MinecraftTag(refs).getAsString();
    }

    @Override
    public byte[] getAsByteArray() {
        return refs.getAsByteArray();
    }
}
