package dev.huskuraft.universal.api.tag;

import dev.huskuraft.universal.api.platform.TagFactory;

/**
 * 字节数组标签（{@code TAG_BYTE_ARRAY}）。
 * <p>
 * 方块实体的 NBT 里常见（潜影盒物品、凋灵骨架等），
 * 序列化时必须支持，否则带这类方块实体的结构无法复制。
 */
public interface ByteArrayTag extends Tag {

    static ByteArrayTag of(byte[] value) {
        return TagFactory.getInstance().newByteArray(value);
    }

    byte[] getAsByteArray();
}
