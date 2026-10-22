package com.mcoo.buildwithease.screen.clipboard;

import java.util.function.Consumer;

import com.mcoo.buildwithease.McooBuildwithEaseClient;
import dev.huskuraft.universal.api.gui.AbstractPanelScreen;
import dev.huskuraft.universal.api.gui.button.Button;
import dev.huskuraft.universal.api.gui.text.TextWidget;
import dev.huskuraft.universal.api.platform.Entrance;
import dev.huskuraft.universal.api.text.Text;
import com.mcoo.buildwithease.building.clipboard.Snapshot;

public class McooBuildwithEaseStructureSnapshotImportScreen extends AbstractPanelScreen {

    private final Consumer<Snapshot> consumer;

    public McooBuildwithEaseStructureSnapshotImportScreen(Entrance entrance, Consumer<Snapshot> consumer) {
        super(entrance, Text.translate("mcoo_build_with_ease.structure.import.title"), PANEL_WIDTH_42, PANEL_TITLE_HEIGHT_1 + PANEL_BUTTON_ROW_HEIGHT_3);
        this.consumer = consumer;
    }

    private Button importFromLitematicButton;
    private Button importFromMcooBuildwithEaseButton;

    @Override
    protected McooBuildwithEaseClient getEntrance() {
        return (McooBuildwithEaseClient) super.getEntrance();
    }

    @Override
    public void onCreate() {
        addWidget(new TextWidget(getEntrance(), getLeft() + getWidth() / 2, getTop() + PANEL_TITLE_HEIGHT_1 - 10, getScreenTitle().withColor(AbstractPanelScreen.TITLE_COLOR), TextWidget.Gravity.CENTER));

        this.importFromLitematicButton = addWidget(Button.builder(getEntrance(), Text.translate("mcoo_build_with_ease.structure.import.import_litematic", ".litematic"), button -> {
            detach();


        }).setBoundsGrid(getLeft(), getTop(), getWidth(), getHeight(), 2f, 0f, 1f).build());
        this.importFromMcooBuildwithEaseButton = addWidget(Button.builder(getEntrance(), Text.translate("mcoo_build_with_ease.structure.import.import_effortless", ".effortless"), button -> {
            detach();

        }).setBoundsGrid(getLeft(), getTop(), getWidth(), getHeight(), 1f, 0f, 1f).build());

        addWidget(Button.builder(getEntrance(), Text.translate("mcoo_build_with_ease.button.done"), button -> {
            detach();
        }).setBoundsGrid(getLeft(), getTop(), getWidth(), getHeight(), 0f, 0f, 1f).build());

    }

    @Override
    public void onReload() {

    }
}
