package dev.huskuraft.universal.vanilla.core;

import dev.huskuraft.universal.api.core.BlockEntity;
import dev.huskuraft.universal.api.core.BlockPosition;
import dev.huskuraft.universal.api.core.BlockState;
import dev.huskuraft.universal.api.core.World;
import dev.huskuraft.universal.api.tag.RecordTag;
import dev.huskuraft.universal.vanilla.tag.MinecraftRecordTag;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;

public record MinecraftBlockEntity(net.minecraft.world.level.block.entity.BlockEntity refs) implements BlockEntity {

    public static BlockEntity ofNullable(net.minecraft.world.level.block.entity.BlockEntity refs) {
        if (refs == null) return null;
        if (refs instanceof BaseContainerBlockEntity baseContainerBlockEntity) return new MinecraftContainerBlockEntity(baseContainerBlockEntity);
        return new MinecraftBlockEntity(refs);
    }

    @Override
    public BlockState getBlockState() {
        return MinecraftBlockState.ofNullable(refs.getBlockState());
    }

    @Override
    public BlockPosition getBlockPosition() {
        return MinecraftConvertor.toPlatformBlockPosition(refs.getBlockPos());
    }

    @Override
    public World getWorld() {
        return MinecraftWorld.ofNullable(refs.getLevel());
    }

    @Override
    public RecordTag getTag() {
        // 26.2：方块实体的自定义数据用 saveCustomOnly(HolderLookup.Provider) 导出成 CompoundTag
        return MinecraftRecordTag.ofNullable(refs.saveCustomOnly(net.minecraft.core.RegistryAccess.EMPTY));
    }

    @Override
    public void setTag(RecordTag recordTag) {
        // 26.2：读入走 ValueInput（TagValueInput.create 从 CompoundTag 构造）
        var valueInput = net.minecraft.world.level.storage.TagValueInput.create(
                net.minecraft.util.ProblemReporter.DISCARDING,
                net.minecraft.core.RegistryAccess.EMPTY,
                (net.minecraft.nbt.CompoundTag) recordTag.reference());
        refs.loadCustomOnly(valueInput);
    }
}
