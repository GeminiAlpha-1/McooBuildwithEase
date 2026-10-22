package dev.huskuraft.universal.vanilla.core;

import dev.huskuraft.universal.api.core.Block;
import dev.huskuraft.universal.api.core.BlockInteraction;
import dev.huskuraft.universal.api.core.BlockPosition;
import dev.huskuraft.universal.api.core.BlockState;
import dev.huskuraft.universal.api.core.InteractionResult;
import dev.huskuraft.universal.api.core.Item;
import dev.huskuraft.universal.api.core.ItemStack;
import dev.huskuraft.universal.api.core.Player;
import dev.huskuraft.universal.api.core.ResourceLocation;
import dev.huskuraft.universal.api.core.World;
import dev.huskuraft.universal.api.text.Text;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.context.UseOnContext;

public record MinecraftItem(net.minecraft.world.item.Item refs) implements Item {

    public static Item ofNullable(net.minecraft.world.item.Item refs) {
        if (refs == null) return null;
        if (refs instanceof BlockItem blockItem) return new MinecraftBlockItem(blockItem);
        if (refs instanceof BucketItem bucketItem) return new MinecraftBucketItem(bucketItem);
        // 26.2 起没有 DiggerItem 这个类了：工具属性是物品组件 Tool，
        // 默认堆栈上带 Tool 组件的物品才算「挖掘类物品」。
        if (refs.getDefaultInstance().has(DataComponents.TOOL)) return new MinecraftDiggerItem(refs);
        return new MinecraftItem(refs);
    }

    @Override
    public ItemStack getDefaultStack() {
        return new MinecraftItemStack(refs.getDefaultInstance());
    }

    @Override
    public Block getBlock() {
        return new MinecraftBlock(net.minecraft.world.level.block.Block.byItem(refs));
    }

    @Override
    public ResourceLocation getId() {
        var minecraftResourceLocation = BuiltInRegistries.ITEM.getKey(refs);
        return new MinecraftResourceLocation(minecraftResourceLocation);
    }

    @Override
    public InteractionResult useOnBlock(Player player, BlockInteraction blockInteraction) {
        return MinecraftConvertor.fromPlatformInteractionResult(refs.useOn(new UseOnContext(player.reference(), MinecraftConvertor.toPlatformInteractionHand(blockInteraction.getHand()), MinecraftConvertor.toPlatformBlockInteraction(blockInteraction))));
    }

    @Override
    public int getMaxStackSize() {
        return refs.getDefaultMaxStackSize();
    }

    @Override
    public boolean isCorrectToolForDrops(BlockState blockState) {
        return refs.isCorrectToolForDrops(refs.getDefaultInstance(), blockState.reference()); // FIXME: 21/6/24
    }

    @Override
    public int getMaxDamage() {
        // 26.2：工具属性是物品组件 Tool，没有 Tool 组件的物品没有耐久消耗。
        var tool = refs.getDefaultInstance().get(DataComponents.TOOL);
        return tool == null ? 0 : tool.damagePerBlock();
    }

    @Override
    public boolean mineBlock(World world, Player player, BlockPosition blockPosition, BlockState blockState, ItemStack itemStack) {
        return refs.mineBlock(itemStack.reference(), world.reference(), blockState.reference(), MinecraftConvertor.toPlatformBlockPosition(blockPosition), player.reference());
    }

    @Override
    public Text getName(ItemStack itemStack) {
        return new MinecraftText(refs.getName(itemStack.reference()));
    }

}
