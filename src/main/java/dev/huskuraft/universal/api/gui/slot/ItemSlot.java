package dev.huskuraft.universal.api.gui.slot;

import dev.huskuraft.universal.api.core.Item;
import dev.huskuraft.universal.api.core.ItemStack;
import dev.huskuraft.universal.api.platform.Entrance;
import dev.huskuraft.universal.api.renderer.Renderer;
import dev.huskuraft.universal.api.text.Text;

public class ItemSlot extends Slot {

    private ItemStack itemStack;

    public ItemSlot(Entrance entrance, int x, int y, int width, int height, Item item, Text message) {
        super(entrance, x, y, width, height, message);
        this.itemStack = item.getDefaultStack();
    }

    public ItemSlot(Entrance entrance, int x, int y, int width, int height, ItemStack itemStack, Text message) {
        super(entrance, x, y, width, height, message);
        this.itemStack = itemStack;
    }

    @Override
    public int getFullWidth() {
        return getWidth();
    }

    @Override
    public void renderWidget(Renderer renderer, int mouseX, int mouseY, float deltaTick) {
        super.renderWidget(renderer, mouseX, mouseY, deltaTick);

//        renderer.drawItemSlotBackgroundTexture(getX() + 1, getY() + 1);
        renderer.renderRect(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0x9f6c6c6c);
        renderer.renderItem(getTypeface(), itemStack, getX(), getY(), getMessage());
    }

    public void setItemStack(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    public void setDescription(Text description) {
        this.setMessage(description);
    }

    private int getBlitOffset() {
        return 0;
    }

}
