package com.mcoo.buildwithease.screen.clipboard;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import com.mcoo.buildwithease.McooBuildwithEase;
import com.mcoo.buildwithease.McooBuildwithEaseClient;
import dev.huskuraft.universal.api.export.StructureExporter;
import dev.huskuraft.universal.api.gui.AbstractPanelScreen;
import dev.huskuraft.universal.api.gui.button.Button;
import dev.huskuraft.universal.api.gui.container.SimpleEntryList;
import dev.huskuraft.universal.api.gui.input.EditBox;
import dev.huskuraft.universal.api.gui.text.TextWidget;
import dev.huskuraft.universal.api.platform.Entrance;
import dev.huskuraft.universal.api.platform.PlatformLoader;
import dev.huskuraft.universal.api.text.Text;

/**
 * 保存结构时的「文件管理器」。
 * <p>
 * 每次导出都开这个界面，让玩家自己挑位置、自己点保存：
 * 默认落在该格式对应的目录（投影/WorldEdit → {@code mcoo_build_with_ease_exports/}，
 * Axiom → {@code Axiom/blueprints/}），但想存到别处分享给别人也完全可以。
 */
public class McooBuildwithEaseSaveFileScreen extends AbstractPanelScreen {

    private static final int LIST_HEIGHT = 140;
    private static final int EDIT_HEIGHT = 20;

    private final List<StructureExporter.Format> formats;
    private final String defaultFileName;
    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;
    private final List<StructureExporter.Block> blocks;

    private Path directory;
    private DirectoryList list;
    private EditBox nameBox;
    private TextWidget pathWidget;

    public McooBuildwithEaseSaveFileScreen(Entrance entrance, StructureExporter.Format format, String defaultFileName,
                                         int sizeX, int sizeY, int sizeZ, List<StructureExporter.Block> blocks) {
        this(entrance, List.of(format), defaultFileName, sizeX, sizeY, sizeZ, blocks, null);
    }

    /**
     * @param formats      要导出的格式（一个就是单格式，多个就一次全导）
     * @param directory    起始目录；为 null 就用该格式的默认目录
     */
    public McooBuildwithEaseSaveFileScreen(Entrance entrance, List<StructureExporter.Format> formats, String defaultFileName,
                                         int sizeX, int sizeY, int sizeZ, List<StructureExporter.Block> blocks,
                                         Path directory) {
        super(entrance, Text.translate("mcoo_build_with_ease.clipboard.download.save_title"), PANEL_WIDTH_60, PANEL_HEIGHT_FULL);
        this.formats = List.copyOf(formats);
        this.defaultFileName = defaultFileName;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.blocks = blocks;
        this.directory = directory != null
                ? directory
                : PlatformLoader.getSingleton(StructureExporter.class).defaultDirectory(this.formats.get(0));
    }

    @Override
    protected McooBuildwithEaseClient getEntrance() {
        return (McooBuildwithEaseClient) super.getEntrance();
    }

    @Override
    public void onCreate() {
        addWidget(new TextWidget(getEntrance(), getLeft() + getWidth() / 2, getTop() + PANEL_TITLE_HEIGHT_1 - 10,
                getScreenTitle().withColor(AbstractPanelScreen.TITLE_COLOR), TextWidget.Gravity.CENTER));

        // Gravity.CENTER 传的是「中心点」，不是左边缘
        this.pathWidget = addWidget(new TextWidget(getEntrance(), getLeft() + getWidth() / 2, getTop() + PANEL_TITLE_HEIGHT_1 + 2,
                getWidth() - PADDINGS_H * 2 - 8, 10, Text.empty(), TextWidget.Gravity.CENTER));

        this.list = addWidget(new DirectoryList(getEntrance(), getLeft() + PADDINGS_H, getTop() + PANEL_TITLE_HEIGHT_1 + 14,
                getWidth() - PADDINGS_H * 2 - 8, LIST_HEIGHT));

        this.nameBox = addWidget(new EditBox(getEntrance(), getLeft() + PADDINGS_H, getTop() + PANEL_TITLE_HEIGHT_1 + 18 + LIST_HEIGHT,
                getWidth() - PADDINGS_H * 2 - 8, EDIT_HEIGHT, Text.empty()));
        this.nameBox.setValue(this.defaultFileName);

        addWidget(Button.builder(getEntrance(), Text.translate("mcoo_build_with_ease.button.save"), button -> {
            save();
        }).setBoundsGrid(getLeft(), getTop(), getWidth(), getHeight(), 0f, 0f, 1 / 3f).build());

        var newFolderButton = Button.builder(getEntrance(), Text.translate("mcoo_build_with_ease.button.new_folder"), button -> {
            createFolder();
        }).setBoundsGrid(getLeft(), getTop(), getWidth(), getHeight(), 0f, 1 / 3f, 1 / 3f).build();
        newFolderButton.setTooltip(Text.translate("mcoo_build_with_ease.button.new_folder.tooltip"));
        addWidget(newFolderButton);

        addWidget(Button.builder(getEntrance(), Text.translate("mcoo_build_with_ease.button.cancel"), button -> {
            detach();
        }).setBoundsGrid(getLeft(), getTop(), getWidth(), getHeight(), 0f, 2 / 3f, 1 / 3f).build());

        refresh();
    }

    /** 重新列出当前目录下的子目录。 */
    private void refresh() {
        this.pathWidget.setMessage(Text.text(shortenPath(this.directory)));
        this.list.clearEntries();
        var parent = this.directory.getParent();
        if (parent != null) {
            this.list.addDirectory(Text.translate("mcoo_build_with_ease.button.parent_folder"), () -> {
                this.directory = parent;
                refresh();
            });
        }
        try (Stream<Path> stream = Files.list(this.directory)) {
            stream.filter(Files::isDirectory)
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .forEach(path -> this.list.addDirectory(Text.text(path.getFileName().toString()), () -> {
                        this.directory = path;
                        refresh();
                    }));
        } catch (IOException exception) {
            McooBuildwithEase.LOGGER.warn("Failed to list directory {}", this.directory, exception);
        }
    }

    /** 路径太长时只留尾部，免得糊出面板。 */
    private static String shortenPath(Path path) {
        var text = path.toString();
        if (text.length() <= 36) {
            return text;
        }
        return "…/" + text.substring(text.length() - 34);
    }

    private void createFolder() {
        var name = this.nameBox.getValue().trim();
        if (name.isEmpty()) {
            return;
        }
        try {
            Files.createDirectory(this.directory.resolve(name));
            this.directory = this.directory.resolve(name);
            this.nameBox.setValue(this.defaultFileName);
            refresh();
        } catch (IOException exception) {
            McooBuildwithEase.LOGGER.warn("Failed to create folder in {}", this.directory, exception);
            getEntrance().getClient().getPlayer().sendMessage(McooBuildwithEase.getSystemMessage(
                    Text.translate("mcoo_build_with_ease.clipboard.download.failed", exception.getMessage() == null ? "" : exception.getMessage())));
        }
    }

    private void save() {
        var player = getEntrance().getClient().getPlayer();
        var name = this.nameBox.getValue().trim();
        if (name.isEmpty()) {
            player.sendMessage(McooBuildwithEase.getSystemMessage(
                    Text.translate("mcoo_build_with_ease.clipboard.download.no_name")));
            return;
        }
        var exporter = PlatformLoader.getSingleton(StructureExporter.class);
        var saved = new java.util.ArrayList<Path>();
        for (var format : this.formats) {
            try {
                saved.add(exporter.export(this.directory, name, this.sizeX, this.sizeY, this.sizeZ, this.blocks, format));
            } catch (Exception exception) {
                McooBuildwithEase.LOGGER.error("Failed to export clipboard to " + format, exception);
                player.sendMessage(McooBuildwithEase.getSystemMessage(
                        Text.translate("mcoo_build_with_ease.clipboard.download.failed",
                                exception.getMessage() == null ? "" : exception.getMessage())));
            }
        }
        if (saved.isEmpty()) {
            return;
        }
        var paths = saved.stream().map(Path::toString).collect(java.util.stream.Collectors.joining("、"));
        player.sendMessage(McooBuildwithEase.getSystemMessage(
                Text.translate("mcoo_build_with_ease.clipboard.download.success", paths)));
        detach();
    }

    /**
     * 目录列表：每行一个子目录，点一下就进去。
     */
    public static class DirectoryList extends SimpleEntryList {

        public DirectoryList(Entrance entrance, int x, int y, int width, int height) {
            super(entrance, x, y, width, height);
            setRenderSelection(false);
        }

        public void addDirectory(Text name, Runnable onClick) {
            addSimpleEntry(entry -> {
                var button = new Button(getEntrance(), entry.getX() + 2, entry.getY(), entry.getWidth() - 4, 12, name, button1 -> onClick.run());
                button.setActive(true);
                entry.addWidget(button);
            });
        }

    }

}
