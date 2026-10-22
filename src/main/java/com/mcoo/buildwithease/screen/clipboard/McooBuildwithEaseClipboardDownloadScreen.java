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
import dev.huskuraft.universal.api.text.Text;

/**
 * 选一种格式导出剪贴板里的结构。
 * <p>
 * 选完格式会进 {@link McooBuildwithEaseSaveFileScreen}（游戏内文件管理器），
 * 由玩家自己挑位置、自己点保存 —— 默认落在该格式对应的目录，但想存到任意路径分享给别人也完全可以。
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
                    openSaveScreen(StructureExporter.Format.LITEMATIC);
                }).setBoundsGrid(getLeft(), getTop(), getWidth(), getHeight(), 3f, 0f, 1f).build());

        addWidget(Button.builder(getEntrance(),
                Text.translate("mcoo_build_with_ease.clipboard.download.worldedit"), button -> {
                    openSaveScreen(StructureExporter.Format.WORLDEDIT);
                }).setBoundsGrid(getLeft(), getTop(), getWidth(), getHeight(), 2f, 0f, 1f).build());

        addWidget(Button.builder(getEntrance(),
                Text.translate("mcoo_build_with_ease.clipboard.download.axiom"), button -> {
                    openSaveScreen(StructureExporter.Format.AXIOM);
                }).setBoundsGrid(getLeft(), getTop(), getWidth(), getHeight(), 1f, 0f, 1f).build());

        addWidget(Button.builder(getEntrance(), Text.translate("mcoo_build_with_ease.button.cancel"), button -> {
            detach();
        }).setBoundsGrid(getLeft(), getTop(), getWidth(), getHeight(), 0f, 0f, 1f).build());
    }

    private void openSaveScreen(StructureExporter.Format format) {
        if (snapshot == null || snapshot.isEmpty()) {
            getEntrance().getClient().getPlayer().sendMessage(McooBuildwithEase.getSystemMessage(
                    Text.translate("mcoo_build_with_ease.clipboard.download.empty")));
            return;
        }
        var box = getBox();
        var blocks = snapshot.blockData().stream()
                .filter(blockData -> blockData.blockState() != null)
                .map(blockData -> new StructureExporter.Block(
                        blockData.blockPosition().x() - box.minX(),
                        blockData.blockPosition().y() - box.minY(),
                        blockData.blockPosition().z() - box.minZ(),
                        blockData.blockState()))
                .toList();

        new McooBuildwithEaseSaveFileScreen(getEntrance(), format, defaultFileName(),
                box.sizeX(), box.sizeY(), box.sizeZ(), List.copyOf(blocks)).attach();
    }

    /** 结构的包围盒（坐标以左下角为原点）。 */
    private Box getBox() {
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
            return new Box(0, 0, 0, 1, 1, 1);
        }
        return new Box(minX, minY, minZ, maxX - minX + 1, maxY - minY + 1, maxZ - minZ + 1);
    }

    private record Box(int minX, int minY, int minZ, int sizeX, int sizeY, int sizeZ) {
    }

    /** 默认文件名（捕捉出来的快照没有名字，补一个带时间戳的，免得每次都覆盖同一个文件）。 */
    private String defaultFileName() {
        var name = snapshot.name();
        if (name != null && !name.isBlank()) {
            return name;
        }
        return "structure_" + java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    }

}
