package dev.huskuraft.universal.api.core;

import dev.huskuraft.universal.api.platform.PlatformReference;
import dev.huskuraft.universal.api.tag.RecordTag;

public interface BlockEntity extends PlatformReference {

    BlockState getBlockState();

    BlockPosition getBlockPosition();

    World getWorld();

    RecordTag getTag();

    void setTag(RecordTag recordTag);

    default BlockEntity copy() {
        var tag = getTag();
        var newBlockEntity = getBlockState().getEntity(getBlockPosition());
        if (newBlockEntity != null) {
            newBlockEntity.setTag(tag);
        }
        return newBlockEntity;
    }

}
