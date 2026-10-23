package dev.huskuraft.universal.api.core;

import dev.huskuraft.universal.api.core.fluid.Fluid;
import dev.huskuraft.universal.api.platform.RegistryFactory;
import dev.huskuraft.universal.api.sound.SoundSet;

public interface BlockState extends StateHolder {

    Registry<BlockState> REGISTRY = RegistryFactory.getInstance().getRegistry();

    BlockState mirror(Axis axis);

    BlockState rotate(Revolve revolve);

    /**
     * 改一个属性，返回新的状态。
     * <p>
     * 用来造一些默认状态之外的变体 —— 比如「含水的树叶」：
     * 默认的树叶不含水，把 {@code waterlogged} 属性打开就是含水树叶。
     *
     * @param property 要改的属性
     * @param value    属性的新值（用 {@link Property#getValue(String)} 取）
     * @return 改完属性的新状态
     */
    BlockState withProperty(Property property, PropertyValue value);

    boolean isAir();

    boolean canBeReplaced(Player player, BlockInteraction interaction);

    boolean isReplaceable();

    boolean hasTagFeatureCannotReplace();

    SoundSet getSoundSet();

    Block getBlock();

    boolean canBeReplaced(Fluid fluid);

    InteractionResult use(Player player, BlockInteraction blockInteraction);

    boolean requiresCorrectToolForDrops();

    default Item getItem() {
        return getBlock().asItem();
    }

    default BlockEntity getEntity(BlockPosition blockPosition) {
        return getBlock().getEntity(blockPosition, this);
    }

    @Deprecated
    int getRequiredItemCount(); // this is a temp solution for slab blocks drop count, will replace with loot table in the future.

}
