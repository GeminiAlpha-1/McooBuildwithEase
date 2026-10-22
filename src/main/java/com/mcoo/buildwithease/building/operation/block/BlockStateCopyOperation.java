package com.mcoo.buildwithease.building.operation.block;

import dev.huskuraft.universal.api.core.BlockInteraction;
import com.mcoo.buildwithease.building.Context;
import com.mcoo.buildwithease.building.Storage;
import com.mcoo.buildwithease.building.operation.Operation;
import com.mcoo.buildwithease.building.operation.empty.EmptyOperation;
import com.mcoo.buildwithease.building.pattern.MirrorContext;
import com.mcoo.buildwithease.building.pattern.MoveContext;
import com.mcoo.buildwithease.building.pattern.RefactorContext;
import com.mcoo.buildwithease.building.pattern.RotateContext;
import com.mcoo.buildwithease.building.session.Session;

public class BlockStateCopyOperation extends BlockOperation {

    public BlockStateCopyOperation(
            Session session,
            Context context,
            Storage storage, // for preview
            BlockInteraction interaction,
            Extras extras
    ) {
        super(session, context, storage, interaction, extras);
    }

    protected BlockOperationResultType saveBlock() {
        if (!context.extras().dimensionId().equals(getWorld().getDimensionId().location())) {
            return BlockOperationResultType.FAIL_WORLD_INCORRECT_DIM;
        }
        if (getPlayer().getGameMode().isSpectator()) {
            return BlockOperationResultType.FAIL_PLAYER_GAME_MODE;
        }
        if (!isInBorderBound()) {
            return BlockOperationResultType.FAIL_WORLD_BORDER;
        }
        if (!isInHeightBound()) {
            return BlockOperationResultType.FAIL_WORLD_HEIGHT;
        }
        if (getBlockState() == null) {
            return BlockOperationResultType.FAIL_BLOCK_STATE_NULL;
        }
        if (getBlockState().isAir() && !context.clipboard().copyAir()) {
            return BlockOperationResultType.FAIL_BLOCK_STATE_AIR;
        }
//        if (!allowInteraction()) {
//            return BlockOperationResultType.FAIL_COPY_NO_PERMISSION;
//        }

        if (!context.configs().constraintConfig().allowCopyPasteStructures()) {
            return BlockOperationResultType.FAIL_COPY_NO_PERMISSION;
        }
        if (!context.configs().constraintConfig().whitelistedItems().isEmpty() && !context.configs().constraintConfig().whitelistedItems().contains(getBlockState().getItem().getId())) {
            return BlockOperationResultType.FAIL_COPY_BLACKLISTED;
        }
        if (!context.configs().constraintConfig().blacklistedItems().isEmpty() && context.configs().constraintConfig().blacklistedItems().contains(getBlockState().getItem().getId())) {
            return BlockOperationResultType.FAIL_COPY_BLACKLISTED;
        }

        if (getWorld().isClient()) {
            return BlockOperationResultType.CONSUME;
        }
        return BlockOperationResultType.SUCCESS;
    }

    @Override
    public BlockStateCopyOperationResult commit() {
        var entityExtrasBeforeOp = Extras.get(getPlayer());
        var blockStateBeforeOp = getBlockStateInWorld();
        var entityTagBeforeOp = getEntityTagInWorld();
        Extras.set(getPlayer(), getExtras());
        var result = saveBlock();
        Extras.set(getPlayer(), entityExtrasBeforeOp);
        var blockStateAfterOp = getBlockStateInWorld();
        var entityTagAfterOp = getEntityTagInWorld();

        if (getContext().isBuildClientType() && getBlockPosition().toVector3d().distance(getPlayer().getEyePosition()) <= 32) {
            getPlayer().getClient().getParticleEngine().crack(getBlockPosition(), getInteraction().getDirection());
        }

        return new BlockStateCopyOperationResult(this, result, blockStateBeforeOp, blockStateAfterOp, entityTagBeforeOp, entityTagAfterOp);
    }

    @Override
    public Operation move(MoveContext moveContext) {
        return new BlockStateCopyOperation(session, context, storage, moveContext.move(interaction), extras);
    }

    @Override
    public Operation mirror(MirrorContext mirrorContext) {
        if (!mirrorContext.isInBounds(getBlockPosition().getCenter())) {
            return new EmptyOperation(context);
        }
        return new BlockStateCopyOperation(session, context, storage, mirrorContext.mirror(interaction), mirrorContext.mirror(extras));
    }

    @Override
    public Operation rotate(RotateContext rotateContext) {
        if (!rotateContext.isInBounds(getBlockPosition().getCenter())) {
            return new EmptyOperation(context);
        }
        return new BlockStateCopyOperation(session, context, storage, rotateContext.rotate(interaction), rotateContext.rotate(extras));
    }

    @Override
    public Operation refactor(RefactorContext source) {
        return this;
    }

    @Override
    public Type getType() {
        return Type.COPY;
    }
}
