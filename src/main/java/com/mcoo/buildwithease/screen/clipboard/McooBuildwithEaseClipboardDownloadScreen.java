package com.mcoo.buildwithease.screen.clipboard;

import java.util.List;

import com.mcoo.buildwithease.McooBuildwithEase;
import com.mcoo.buildwithease.McooBuildwithEaseClient;
import com.mcoo.buildwithease.building.clipboard.Snapshot;
import dev.huskuraft.universal.api.export.StructureExporter;
import dev.huskuraft.universal.api.gui.AbstractPanelScreen;
import dev.huskuraft.universal.api.gui.button.Button;
import dev.huskuraft.universal.api.gui.text.TextWidget;
import dev.huskuraft.universal.api.platform.Entrance;
import dev.huskuraft.universal.api.platform.PlatformLoader;
import dev.huskuraft.universal.api.text.Text;

/**
 * 把剪贴板里的结构导出成工程文件。
 * <p>
 * 原先的「分享」（发给别的玩家）和「编辑」（上游就是空实现）合并成了这一个「下载」。
 */
public class McooBuildwithEaseClipboardDownloadScreen extends AbstractPanelScreen {

    private final Snapshot snapshot;

    public McooBuildwithEaseClipboardDownloadScreen(Entrance entrance, Snapshot snapshot) {
        super(entrance, Text.translate("mcoo_build_with_ease.clipboard.download.title"), PANEL_WIDTH_60,
                PANEL_TITLE_HEIGHT_1 + PANEL_BUTTON_ROW_HEIGHT_4);
        this.snapshot = snapshot;
    }

    @Override
    protected McooBuildwithEaseClient getEntrance() {
        return (McooBuildwithEaseClient) super.getEntrance();
    }

    @Override
    public void onCreate() {
        addWidget(new TextWidget(getEntrance(), getLeft() + getWidth() / 2, getTop() + PANEL_TITLE_HEIGHT_1 - 10,
                getScreenTitle().withColor(AbstractPanelScreen.TITLE_COLOR), TextWidget.Gravity.CENTER));

        addWidget(Button.builder(getEntrance(),
                Text.translate("mcoo_build_with_ease.clipboard.download.litematic"), button -> {
                    export(StructureExporter.Format.LITEMATIC);
                }).setBoundsGrid(getLeft(), getTop(), getWidth(), getHeight(), 3f, 0f, 1f).build());

        addWidget(Button.builder(getEntrance(),
                Text.translate("mcoo_build_with_ease.clipboard.download.worldedit"), button -> {
                    export(StructureExporter.Format.WORLDEDIT);
                }).setBoundsGrid(getLeft(), getTop(), getWidth(), getHeight(), 2f, 0f, 1f).build());

        addWidget(Button.builder(getEntrance(),
                Text.translate("mcoo_build_with_ease.clipboard.download.axiom"), button -> {
                    export(StructureExporter.Format.AXIOM);
                }).setBoundsGrid(getLeft(), getTop(), getWidth(), getHeight(), 1f, 0f, 1f).build());

        addWidget(Button.builder(getEntrance(), Text.translate("mcoo_build_with_ease.button.cancel"), button -> {
            detach();
        }).setBoundsGrid(getLeft(), getTop(), getWidth(), getHeight(), 0f, 0f, 1f).build());
    }

    private void export(StructureExporter.Format format) {
        var player = getEntrance().getClient().getPlayer();
        if (snapshot == null || snapshot.isEmpty()) {
            player.sendMessage(McooBuildwithEase.getSystemMessage(
                    Text.translate("mcoo_build_with_ease.clipboard.download.empty")));
            return;
        }
        try {
            var path = doExport(format);
            player.sendMessage(McooBuildwithEase.getSystemMessage(
                    Text.translate("mcoo_build_with_ease.clipboard.download.success", path.toString())));
            detach();
        } catch (Exception exception) {
            McooBuildwithEase.LOGGER.error("Failed to export clipboard to " + format, exception);
            player.sendMessage(McooBuildwithEase.getSystemMessage(
                    Text.translate("mcoo_build_with_ease.clipboard.download.failed", exception.getMessage() == null ? "" : exception.getMessage())));
        }
    }

    private java.nio.file.Path doExport(StructureExporter.Format format) {
        var minX = Integer.MAX_VALUE;
        var minY = Integer.MAX_VALUE;
        var minZ = Integer.MAX_VALUE;
        var maxX = Integer.MIN_VALUE;
        var maxY = Integer.MIN_VALUE;
        var maxZ = Integer.MIN_VALUE;
        for (var blockData : snapshot.blockData()) {
            var position = blockData.blockPosition();
            minX = Math.min(minX, position.x());
            minY = Math.min(minY, position.y());
            minZ = Math.min(minZ, position.z());
            maxX = Math.max(maxX, position.x());
            maxY = Math.max(maxY, position.y());
            maxZ = Math.max(maxZ, position.z());
        }
        if (minX > maxX) {
            minX = minY = minZ = 0;
            maxX = maxY = maxZ = 0;
        }
        final var originX = minX;
        final var originY = minY;
        final var originZ = minZ;
        var sizeX = maxX - minX + 1;
        var sizeY = maxY - minY + 1;
        var sizeZ = maxZ - minZ + 1;

        var blocks = snapshot.blockData().stream()
                .filter(blockData -> blockData.blockState() != null)
                .map(blockData -> new StructureExporter.Block(
                        blockData.blockPosition().x() - originX,
                        blockData.blockPosition().y() - originY,
                        blockData.blockPosition().z() - originZ,
                        blockData.blockState()))
                .toList();

        var exporter = PlatformLoader.getSingleton(StructureExporter.class);
        var name = snapshot.name() == null || snapshot.name().isBlank() ? "structure" : snapshot.name();
        return exporter.export(name, sizeX, sizeY, sizeZ, List.copyOf(blocks), format);
    }

}
