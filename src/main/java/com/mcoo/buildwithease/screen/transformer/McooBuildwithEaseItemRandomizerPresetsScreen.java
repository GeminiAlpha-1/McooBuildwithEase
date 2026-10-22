package com.mcoo.buildwithease.screen.transformer;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.mcoo.buildwithease.McooBuildwithEaseClient;
import dev.huskuraft.universal.api.gui.AbstractPanelScreen;
import dev.huskuraft.universal.api.gui.button.Button;
import dev.huskuraft.universal.api.gui.text.TextWidget;
import dev.huskuraft.universal.api.gui.tooltip.TooltipHelper;
import dev.huskuraft.universal.api.platform.Entrance;
import dev.huskuraft.universal.api.text.ChatFormatting;
import dev.huskuraft.universal.api.text.Text;
import com.mcoo.buildwithease.building.config.PatternConfig;
import com.mcoo.buildwithease.building.pattern.Transformer;
import com.mcoo.buildwithease.building.pattern.randomize.ItemRandomizer;

public class McooBuildwithEaseItemRandomizerPresetsScreen extends AbstractPanelScreen {

    private Consumer<List<? extends Transformer>> consumer;
    private List<? extends Transformer> config;
    private TransformerList entries;
    private TextWidget titleTextWidget;
    private Button editButton;
    private Button deleteButton;
    private Button clearButton;
    private Button addButton;
    private Button cancelButton;
    private Button saveButton;

    public McooBuildwithEaseItemRandomizerPresetsScreen(Entrance entrance) {
        super(entrance, Text.translate("mcoo_build_with_ease.transformer_presets.title").withStyle(ChatFormatting.DARK_GRAY), PANEL_WIDTH_60, PANEL_HEIGHT_FULL);
        this.consumer = transformers -> {
            getEntrance().getConfigStorage().update(config -> config.withPatternConfig(new PatternConfig(transformers)));
        };
        this.config = getEntrance().getConfigStorage().get().patternConfig().itemRandomizers();
    }

    public McooBuildwithEaseItemRandomizerPresetsScreen(Entrance entrance, Consumer<List<? extends Transformer>> consumer) {
        this(entrance);
        this.consumer = consumer;
    }

    @Override
    protected McooBuildwithEaseClient getEntrance() {
        return (McooBuildwithEaseClient) super.getEntrance();
    }

    @Override
    public void onCreate() {

        this.titleTextWidget = addWidget(new TextWidget(getEntrance(), getLeft() + getWidth() / 2, getTop() + PANEL_TITLE_HEIGHT_1 - 10, getScreenTitle().withColor(AbstractPanelScreen.TITLE_COLOR), TextWidget.Gravity.CENTER));

        this.editButton = addWidget(Button.builder(getEntrance(), Text.translate("mcoo_build_with_ease.button.edit"), button -> {
            if (entries.getSelected() != null && !entries.getSelected().getItem().isBuiltIn()) {
                editTransformer((ItemRandomizer) entries.getSelected().getItem());
            }
        }).setBoundsGrid(getLeft(), getTop(), getWidth(), getHeight(), 1f, 0f, 0.25f).build());

        this.deleteButton = addWidget(Button.builder(getEntrance(), Text.translate("mcoo_build_with_ease.button.delete"), button -> {
            if (entries.getSelected() != null && !entries.getSelected().getItem().isBuiltIn()) {
                entries.deleteSelected();
            }
        }).setBoundsGrid(getLeft(), getTop(), getWidth(), getHeight(), 1f, 0.25f, 0.25f).build());


        this.clearButton = addWidget(Button.builder(getEntrance(), Text.translate("mcoo_build_with_ease.button.clear"), button -> {
            this.entries.reset(this.entries.items().stream().filter(transformer1 -> transformer1.isBuiltIn()).toList());
        }).setBoundsGrid(getLeft(), getTop(), getWidth(), getHeight(), 1f, 0.5f, 0.25f).build());


        this.addButton = addWidget(Button.builder(getEntrance(), Text.translate("mcoo_build_with_ease.button.add"), button -> {
            editTransformer(ItemRandomizer.EMPTY.withName(Text.empty()).withRandomId());
        }).setBoundsGrid(getLeft(), getTop(), getWidth(), getHeight(), 1f, 0.75f, 0.25f).build());

        this.cancelButton = addWidget(Button.builder(getEntrance(), Text.translate("mcoo_build_with_ease.button.cancel"), button -> {
            detach();
        }).setBoundsGrid(getLeft(), getTop(), getWidth(), getHeight(), 0f, 0f, 0.5f).build());
        this.saveButton = addWidget(Button.builder(getEntrance(), Text.translate("mcoo_build_with_ease.button.save"), button -> {
            detach();
            consumer.accept(config);

        }).setBoundsGrid(getLeft(), getTop(), getWidth(), getHeight(), 0f, 0.5f, 0.5f).build());


        this.entries = addWidget(new TransformerList(getEntrance(), getLeft() + AbstractPanelScreen.PADDINGS_H, getTop() + PANEL_TITLE_HEIGHT_1, getWidth() - AbstractPanelScreen.PADDINGS_H * 2 - 8 /* scrollbar */, getHeight() - PANEL_TITLE_HEIGHT_1 - PANEL_BUTTON_ROW_HEIGHT_2));
        this.entries.setAlwaysShowScrollbar(true);
        this.entries.reset(this.config);

    }

    @Override
    public void onReload() {
        this.editButton.setActive(entries.getSelected() != null && !entries.getSelected().getItem().isBuiltIn());
        this.deleteButton.setActive(entries.getSelected() != null && !entries.getSelected().getItem().isBuiltIn());

        if (entries.getSelected() != null && entries.getSelected().getItem().isBuiltIn()) {
            this.editButton.setTooltip(
                    Stream.concat(
                            Stream.of(Text.translate("mcoo_build_with_ease.transformer.edit.tooltip.cannot_edit_built_in.title")),
                            TooltipHelper.wrapLines(getTypeface(), Text.translate("mcoo_build_with_ease.transformer.edit.tooltip.cannot_edit_built_in.message").withStyle(ChatFormatting.GRAY)).stream()
                    ).toList()
            );
        } else {
            this.editButton.clearTooltip();
        }
        if (entries.getSelected() != null && entries.getSelected().getItem().isBuiltIn()) {
            this.deleteButton.setTooltip(
                    Stream.concat(
                            Stream.of(Text.translate("mcoo_build_with_ease.transformer.delete.tooltip.cannot_delete_built_in.title")),
                            TooltipHelper.wrapLines(getTypeface(), Text.translate("mcoo_build_with_ease.transformer.delete.tooltip.cannot_delete_built_in.message").withStyle(ChatFormatting.GRAY)).stream()
                    ).toList()
            );
        } else {
            this.deleteButton.clearTooltip();
        }
        this.addButton.setTooltip(
                Stream.concat(
                        Stream.of(Text.translate("mcoo_build_with_ease.transformer.add.tooltip.random.title")),
                        TooltipHelper.wrapLines(getTypeface(), Text.translate("mcoo_build_with_ease.transformer.add.tooltip.random.message").withStyle(ChatFormatting.GRAY)).stream()
                ).toList()
        );

        if (entries.consumeDoubleClick() && entries.hasSelected()) {
            if (!entries.getSelected().getItem().isBuiltIn()) {
                editTransformer((ItemRandomizer) entries.getSelected().getItem());
            }
        }
        this.config = entries.items();

    }

    private void editTransformer(ItemRandomizer transformer) {
        new McooBuildwithEaseItemRandomizerEditScreen(
                getEntrance(),
                result -> {
                    if (entries.hasSelected()) {
                        entries.replaceSelect(result.withName(Text.empty()));
                    } else {
                        entries.insertSelected(result.withName(Text.empty()));
                    }
                    onReload();
                },
                transformer
        ).attach();
    }

}

