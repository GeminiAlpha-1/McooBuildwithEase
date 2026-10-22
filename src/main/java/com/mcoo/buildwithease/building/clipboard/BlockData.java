package com.mcoo.buildwithease.building.clipboard;

import dev.huskuraft.universal.api.core.BlockEntity;
import dev.huskuraft.universal.api.core.BlockPosition;
import dev.huskuraft.universal.api.core.BlockState;
import dev.huskuraft.universal.api.tag.RecordTag;
import com.mcoo.buildwithease.building.operation.Mirrorable;
import com.mcoo.buildwithease.building.operation.Movable;
import com.mcoo.buildwithease.building.operation.Rotatable;
import com.mcoo.buildwithease.building.pattern.MirrorContext;
import com.mcoo.buildwithease.building.pattern.MoveContext;
import com.mcoo.buildwithease.building.pattern.RotateContext;

public record BlockData(
        BlockPosition blockPosition,
        BlockState blockState,
        RecordTag entityTag
) implements Rotatable<BlockData>, Movable<BlockData>, Mirrorable<BlockData> {

    @Override
    public BlockData rotate(RotateContext rotateContext) {
        return new BlockData(
                rotateContext.rotate(blockPosition),
                rotateContext.rotate(blockState),
                entityTag
        );
    }

    @Override
    public BlockData move(MoveContext moveContext) {
        return new BlockData(
                moveContext.move(blockPosition),
                blockState,
                entityTag
        );
    }

    @Override
    public BlockData mirror(MirrorContext mirrorContext) {
        return new BlockData(
                mirrorContext.mirror(blockPosition),
                mirrorContext.mirror(blockState),
                entityTag
        );
    }

    public BlockEntity blockEntity() {
        if (blockState == null) {
            return null;
        }
        var blockEntity = blockState.getEntity(blockPosition);
        if (blockEntity == null) {
            return null;
        }
        if (entityTag != null) {
            blockEntity.setTag(entityTag);
        }
        return blockEntity;
    }
}
