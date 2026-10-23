package com.mcoo.buildwithease.screen.clipboard;

import com.mcoo.buildwithease.McooBuildwithEaseClient;
import dev.huskuraft.universal.api.gui.AbstractPanelScreen;
import dev.huskuraft.universal.api.gui.button.Button;
import dev.huskuraft.universal.api.gui.text.TextWidget;
import dev.huskuraft.universal.api.platform.Entrance;
import dev.huskuraft.universal.api.text.Text;

/**
 * 导出预览图片的分辨率选择。
 * <p>
 * 导出的是**透明背景**的 PNG，画面和预览里看到的完全一致。
 */
public class McooBuildwithEaseExportImageScreen extends AbstractPanelScreen {

    private final StructureSnapshotWidget widget;

    public McooBuildwithEaseExportImageScreen(Entrance entrance, StructureSnapshotWidget widget) {
        super(entrance, Text.translate("mcoo_build_with_ease.clipboard.export_image.title"), PANEL_WIDTH_60,
                PANEL_TITLE_HEIGHT_1 + PANEL_BUTTON_ROW_HEIGHT_4);
        this.widget = widget;
    }

    @Override
    protected McooBuildwithEaseClient getEntrance() {
        return (McooBuildwithEaseClient) super.getEntrance();
    }

    @Override
    public void onCreate() {
        addWidget(new TextWidget(getEntrance(), getLeft() + getWidth() / 2, getTop() + PANEL_TITLE_HEIGHT_1 - 10,
                getScreenTitle().withColor(AbstractPanelScreen.TITLE_COLOR), TextWidget.Gravity.CENTER));

        addWidget(Button.builder(getEntrance(), Text.translate("mcoo_build_with_ease.clipboard.export_image.1k"), button -> {
            export(1024);
        }).setBoundsGrid(getLeft(), getTop(), getWidth(), getHeight(), 3f, 0f, 1f).build());

        addWidget(Button.builder(getEntrance(), Text.translate("mcoo_build_with_ease.clipboard.export_image.2k"), button -> {
            export(2048);
        }).setBoundsGrid(getLeft(), getTop(), getWidth(), getHeight(), 2f, 0f, 1f).build());

        addWidget(Button.builder(getEntrance(), Text.translate("mcoo_build_with_ease.clipboard.export_image.4k"), button -> {
            export(4096);
        }).setBoundsGrid(getLeft(), getTop(), getWidth(), getHeight(), 1f, 0f, 1f).build());

        addWidget(Button.builder(getEntrance(), Text.translate("mcoo_build_with_ease.button.cancel"), button -> {
            detach();
        }).setBoundsGrid(getLeft(), getTop(), getWidth(), getHeight(), 0f, 0f, 1f).build());
    }

    /** 按长边算分辨率，宽高比跟预览区一致。 */
    private void export(int longEdge) {
        var width = Math.max(1, this.widget.getWidth());
        var height = Math.max(1, this.widget.getHeight());
        var targetWidth = longEdge;
        var targetHeight = Math.max(1, Math.round((float) longEdge * height / width));
        this.widget.exportImage(targetWidth, targetHeight);
        detach();
    }
}
