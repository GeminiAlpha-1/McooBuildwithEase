package dev.huskuraft.universal.api.platform;

import dev.huskuraft.universal.api.tag.ListTag;
import dev.huskuraft.universal.api.tag.StringTag;
import dev.huskuraft.universal.api.tag.NumericTag;
import dev.huskuraft.universal.api.tag.RecordTag;

public interface TagFactory {

    static TagFactory getInstance() {
        return PlatformLoader.getSingleton();
    }

    RecordTag newRecord();

    ListTag newList();

    StringTag newLiteral(String value);

    NumericTag newPrimitive(boolean value);

    NumericTag newPrimitive(byte value);

    NumericTag newPrimitive(short value);

    NumericTag newPrimitive(int value);

    NumericTag newPrimitive(long value);

    NumericTag newPrimitive(float value);

    NumericTag newPrimitive(double value);


}
