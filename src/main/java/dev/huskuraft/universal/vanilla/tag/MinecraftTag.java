package dev.huskuraft.universal.vanilla.tag;

import dev.huskuraft.universal.api.tag.Tag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;

public record MinecraftTag(net.minecraft.nbt.Tag refs) implements Tag {

    public static Tag ofNullable(net.minecraft.nbt.Tag refs) {
        if (refs == null) return null;
        if (refs instanceof NumericTag numericTag) return new MinecraftNumericTag(numericTag);
        if (refs instanceof StringTag stringTag) return new MinecraftStringTag(stringTag);
        if (refs instanceof CompoundTag compoundTag) return new MinecraftRecordTag(compoundTag);
        if (refs instanceof ListTag listTag) return new MinecraftListTag(listTag);
        // 数组类型必须单独包装：通用 MinecraftTag 拿不到数组数据，
        // 序列化时会被当成不支持的 tag 抛异常。
        if (refs instanceof net.minecraft.nbt.ByteArrayTag byteArrayTag) return new MinecraftByteArrayTag(byteArrayTag);
        if (refs instanceof net.minecraft.nbt.IntArrayTag intArrayTag) return new MinecraftIntArrayTag(intArrayTag);
        if (refs instanceof net.minecraft.nbt.LongArrayTag longArrayTag) return new MinecraftLongArrayTag(longArrayTag);
        return new MinecraftTag(refs);
    }

    @Override
    public byte getId() {
        return refs.getId();
    }

    @Override
    public String getAsString() {
        // 26.2：Tag#getAsString() 改成了 asString()（Optional）
        return refs.asString().orElseGet(refs::toString);
    }
}
