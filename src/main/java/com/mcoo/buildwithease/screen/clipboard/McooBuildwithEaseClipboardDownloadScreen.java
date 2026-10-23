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
                PANEL_TITLE_HEIGHT_1 + PANEL_BUTTON_ROW_HEIGHT_5);
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
                    openSaveScreen(List.of(StructureExporter.Format.LITEMATIC));
                }).setBoundsGrid(getLeft(), getTop(), getWidth(), getHeight(), 4f, 0f, 1f).build());

        addWidget(Button.builder(getEntrance(),
                Text.translate("mcoo_build_with_ease.clipboard.download.worldedit"), button -> {
                    openSaveScreen(List.of(StructureExporter.Format.WORLDEDIT));
                }).setBoundsGrid(getLeft(), getTop(), getWidth(), getHeight(), 3f, 0f, 1f).build());

        addWidget(Button.builder(getEntrance(),
                Text.translate("mcoo_build_with_ease.clipboard.download.axiom"), button -> {
                    openSaveScreen(List.of(StructureExporter.Format.AXIOM));
                }).setBoundsGrid(getLeft(), getTop(), getWidth(), getHeight(), 2f, 0f, 1f).build());

        // 「下载到电脑的下载文件夹」：起始目录是系统的下载文件夹，保存时两种格式一起导出
        addWidget(Button.builder(getEntrance(),
                Text.translate("mcoo_build_with_ease.clipboard.download.to_downloads"), button -> {
                    openSaveScreen(List.of(StructureExporter.Format.LITEMATIC, StructureExporter.Format.WORLDEDIT),
                            downloadsDirectory());
                }).setBoundsGrid(getLeft(), getTop(), getWidth(), getHeight(), 1f, 0f, 1f).build());

        addWidget(Button.builder(getEntrance(), Text.translate("mcoo_build_with_ease.button.cancel"), button -> {
            detach();
        }).setBoundsGrid(getLeft(), getTop(), getWidth(), getHeight(), 0f, 0f, 1f).build());
    }

    /**
     * 系统里「下载」文件夹的位置。
     * <p>
     * Windows / macOS / Linux 上一般叫 {@code Downloads}；
     * 个别本地化环境叫「下载」，两个都找一下。
     */
    private static java.nio.file.Path downloadsDirectory() {
        var home = java.nio.file.Path.of(System.getProperty("user.home", "."));
        for (var name : new String[]{"Downloads", "下载"}) {
            var candidate = home.resolve(name);
            if (java.nio.file.Files.isDirectory(candidate)) {
                return candidate;
            }
        }
        return home.resolve("Downloads");
    }

    private void openSaveScreen(List<StructureExporter.Format> formats) {
        openSaveScreen(formats, null);
    }

    /**
     * 打开内置的文件管理器。
     *
     * @param formats   要导出的格式（可以多个，保存时一起导出）
     * @param directory 起始目录；为 null 就用该格式的默认目录
     *                  （装了对应的 mod 用它的目录，没装就放模组自己的导出目录）
     */
    private void openSaveScreen(List<StructureExporter.Format> formats, java.nio.file.Path directory) {
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

        new McooBuildwithEaseSaveFileScreen(getEntrance(), formats, defaultFileName(),
                box.sizeX(), box.sizeY(), box.sizeZ(), List.copyOf(blocks), directory).attach();
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
