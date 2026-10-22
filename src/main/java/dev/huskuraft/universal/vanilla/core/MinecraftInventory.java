package dev.huskuraft.universal.vanilla.core;

import dev.huskuraft.universal.api.core.Inventory;
import dev.huskuraft.universal.api.core.ItemStack;
import net.minecraft.world.entity.EquipmentSlot;

public record MinecraftInventory(net.minecraft.world.entity.player.Inventory refs) implements Inventory {

    /**
     * 26.2 起护甲和副手不再放在 Inventory 里，而是放到了玩家的装备（EntityEquipment）上：
     * {@code Inventory} 只剩主物品栏（{@code getNonEquipmentItems()}），
     * 护甲走 {@link EquipmentSlot}，副手走 {@link EquipmentSlot#OFFHAND}。
     *
     * <p>common-api 的 Inventory 抽象仍然是「扁平的 [背包 | 护甲 | 副手]」，
     * 这里按同样的布局做映射，业务层不用改。
     */
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };

    @Override
    public ItemStack getItem(int index) {
        var bagSize = getBagSize();
        if (index < 0 || index >= getContainerSize()) {
            return ItemStack.empty();
        }
        if (index < bagSize) {
            return new MinecraftItemStack(refs.getNonEquipmentItems().get(index));
        }
        var armorIndex = index - bagSize;
        if (armorIndex < getArmorSize()) {
            return new MinecraftItemStack(refs.player.getItemBySlot(ARMOR_SLOTS[armorIndex]));
        }
        return new MinecraftItemStack(refs.player.getItemBySlot(EquipmentSlot.OFFHAND));
    }

    @Override
    public void setItem(int index, ItemStack itemStack) {
        var bagSize = getBagSize();
        if (index < 0 || index >= getContainerSize()) {
            return;
        }
        if (index < bagSize) {
            refs.getNonEquipmentItems().set(index, itemStack.reference());
            return;
        }
        var armorIndex = index - bagSize;
        if (armorIndex < getArmorSize()) {
            refs.player.setItemSlot(ARMOR_SLOTS[armorIndex], itemStack.reference());
            return;
        }
        refs.player.setItemSlot(EquipmentSlot.OFFHAND, itemStack.reference());
    }

    @Override
    public int getBagSize() {
        return refs.getNonEquipmentItems().size();
    }

    @Override
    public int getArmorSize() {
        return ARMOR_SLOTS.length;
    }

    @Override
    public int getOffhandSize() {
        return 1;
    }

    @Override
    public int getHotbarSize() {
        return net.minecraft.world.entity.player.Inventory.getSelectionSize();
    }

    @Override
    public int getSelected() {
        return refs.getSelectedSlot();
    }

}
