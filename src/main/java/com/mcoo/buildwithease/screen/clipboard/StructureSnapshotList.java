package com.mcoo.buildwithease.screen.clipboard;

import dev.huskuraft.universal.api.gui.Dimens;
import dev.huskuraft.universal.api.gui.container.EditableEntryList;
import dev.huskuraft.universal.api.gui.slot.ItemSlot;
import dev.huskuraft.universal.api.gui.text.TextWidget;
import dev.huskuraft.universal.api.platform.Entrance;
import dev.huskuraft.universal.api.text.ChatFormatting;
import dev.huskuraft.universal.api.text.Text;
import com.mcoo.buildwithease.building.clipboard.Snapshot;

import java.util.UUID;

public final class StructureSnapshotList extends EditableEntryList<Snapshot> {

    public StructureSnapshotList(Entrance entrance, int x, int y, int width, int height) {
        super(entrance, x, y, width, height);
    }

    @Override
    protected SnapshotEntry createHolder(Snapshot Snapshot) {
        return new SnapshotEntry(getEntrance(), Snapshot);
    }

    private static class SnapshotEntry extends Entry<Snapshot> {

        private StructureSnapshotWidget snapshotWidget;
        private TextWidget textWidget;
        private TextWidget uuidTextWidget;
        /**
         * 图标网格实际占多高（像素）。
         * <p>
         * 条目高度必须和它一致 —— 不然条目自己的裁剪区会把下面的图标切掉，
         * 列表的滚动范围也算不准（大结构就滚不到底）。
         */
        private int gridHeight;

        public SnapshotEntry(Entrance entrance, Snapshot snapshot) {
            super(entrance, snapshot);
        }

        @Override
        public void onCreate() {
            this.snapshotWidget = addWidget(new StructureSnapshotWidget(getEntrance(), getX() + 1, getY() + 1, Dimens.Icon.SIZE_66, Dimens.Icon.SIZE_66, getItem()));
            this.snapshotWidget.setBackgroundColor(0x9f6c6c6c);
            this.textWidget = addWidget(new TextWidget(getEntrance(), getX() + 6 + Dimens.Icon.SIZE_66, getY() + 4, Text.text("Structure Snapshot")));
            this.uuidTextWidget = addWidget(new TextWidget(getEntrance(), getX() + 6 + Dimens.Icon.SIZE_66, getY() + 4 + 11, Text.text(UUID.randomUUID().toString()).withStyle(ChatFormatting.GRAY)));

            int xOffset = 0;
            int yOffset = 0;
            for (var itemStack : getItem().getItems()) {
                addWidget(new ItemSlot(getEntrance(), getX() + 5 + Dimens.Icon.SIZE_66 + xOffset, getY() + 4 + 11 + 11 + yOffset, Dimens.SLOT_WIDTH, Dimens.SLOT_HEIGHT, itemStack, Text.text(String.valueOf(itemStack.getCount()))));
                xOffset += Dimens.SLOT_WIDTH + 2;
                if (xOffset > getWidth() - Dimens.Icon.SIZE_66 - 5 - 20) {
                    xOffset = 0;
                    yOffset += Dimens.SLOT_HEIGHT + 2;
                }
            }
            // 图标网格占的高度：起始位置（标题 + UUID 两行）+ 每一行的高度
            this.gridHeight = getItem().getItems().isEmpty() ? 0
                    : 4 + 11 + 11 + yOffset + Dimens.SLOT_HEIGHT;
        }

        @Override
        public void onReload() {

        }

        /**
         * 条目高度 = 图标网格实际占的高度。
         * <p>
         * 必须和实际排布一致：
         * <ul>
         *     <li>高度不够 → 条目自己的裁剪区会把下面的图标切掉（大结构尤其明显）；</li>
         *     <li>高度不够 → 列表算出来的滚动范围也不对，滚不到底。</li>
         * </ul>
         * 所以这里始终按网格的实际行数算高度，列表用滚轮上下翻。
         */
        @Override
        public int getHeight() {
            return Math.max(Dimens.Icon.SIZE_66 + 6, this.gridHeight + 4);
        }

    }
}
