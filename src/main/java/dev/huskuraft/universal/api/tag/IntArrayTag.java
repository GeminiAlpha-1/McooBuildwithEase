package dev.huskuraft.universal.api.tag;

import dev.huskuraft.universal.api.platform.TagFactory;

/**
 * int 数组标签（{@code TAG_INT_ARRAY}）。
 */
public interface IntArrayTag extends Tag {

    static IntArrayTag of(int[] value) {
        return TagFactory.getInstance().newIntArray(value);
    }

    int[] getAsIntArray();
}
