package dev.huskuraft.universal.vanilla.tag;

import dev.huskuraft.universal.api.tag.NumericTag;

public record MinecraftNumericTag(net.minecraft.nbt.NumericTag refs) implements NumericTag {

    @Override
    public byte getId() {
        return new MinecraftTag(refs).getId();
    }

    @Override
    public String getAsString() {
        return new MinecraftTag(refs).getAsString();
    }

    // 26.2 起 NBT 的数值读取改成了 java.lang.Number 风格的访问器

    @Override
    public long getAsLong() {
        return refs.longValue();
    }

    @Override
    public int getAsInt() {
        return refs.intValue();
    }

    @Override
    public short getAsShort() {
        return refs.shortValue();
    }

    @Override
    public byte getAsByte() {
        return refs.byteValue();
    }

    @Override
    public double getAsDouble() {
        return refs.doubleValue();
    }

    @Override
    public float getAsFloat() {
        return refs.floatValue();
    }

    @Override
    public Number getAsNumber() {
        return refs.box();
    }

}
