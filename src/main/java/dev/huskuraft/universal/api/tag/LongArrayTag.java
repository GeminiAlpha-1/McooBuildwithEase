package dev.huskuraft.universal.api.tag;

import dev.huskuraft.universal.api.platform.TagFactory;

/**
 * long 数组标签（{@code TAG_LONG_ARRAY}）。
 * <p>
 * Minecraft 26.2 新增的标签类型。
 */
public interface LongArrayTag extends Tag {

    static LongArrayTag of(long[] value) {
        return TagFactory.getInstance().newLongArray(value);
    }

    long[] getAsLongArray();
}
