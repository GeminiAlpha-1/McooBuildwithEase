package com.mcoo.buildwithease.screen.pattern;

import java.util.List;
import java.util.function.Consumer;

import com.mcoo.buildwithease.McooBuildwithEaseClient;
import dev.huskuraft.universal.api.gui.AbstractPanelScreen;
import dev.huskuraft.universal.api.gui.button.Button;
import dev.huskuraft.universal.api.gui.text.TextWidget;
import dev.huskuraft.universal.api.platform.Entrance;
import dev.huskuraft.universal.api.text.Text;
import com.mcoo.buildwithease.building.config.PatternConfig;
import com.mcoo.buildwithease.screen.settings.SettingOptionsList;
import com.mcoo.buildwithease.screen.transformer.McooBuildwithEaseItemRandomizerPresetsScreen;

public class McooBuildwithEasePatternSettingsScreen extends AbstractPanelScreen {

    private final Consumer<PatternConfig> consumer;
    private PatternConfig originalConfig;
    private PatternConfig config;

    public McooBuildwithEasePatternSettingsScreen(Entrance entrance) {
        super(entrance, Text.translate("mcoo_build_with_ease.pattern_settings.title"), PANEL_WIDTH_60, PANEL_HEIGHT_FULL);
        this.consumer = newConfig -> {
            getEntrance().getConfigStorage().update(config -> config.withPatternConfig(newConfig));
        };
        this.config = getEntrance().getConfigStorage().get().patternConfig();
        this.originalConfig = config;
    }

    @Override
    public void onCreate() {
        addWidget(new TextWidget(getEntrance(), getLeft() + getWidth() / 2, getTop() + PANEL_TITLE_HEIGHT_1 - 10, getScreenTitle().withColor(AbstractPanelScreen.TITLE_COLOR), TextWidget.Gravity.CENTER));


        var entries = addWidget(new SettingOptionsList(getEntrance(), getLeft() + PADDINGS_H, getTop() + PANEL_TITLE_HEIGHT_1, getWidth() - PADDINGS_H * 2 - 8, getHeight() - PANEL_TITLE_HEIGHT_1 - PANEL_BUTTON_ROW_HEIGHT_1, false, false));

        entries.addTab(Text.translate("mcoo_build_with_ease.pattern_settings.item_randomizer_presets"), null, config.transformerPreset(), (value) -> {
            this.config = new PatternConfig(value);
        }, (entry, value) -> {
            entry.getButton().setOnPressListener(button1 -> {
                new McooBuildwithEaseItemRandomizerPresetsScreen(getEntrance(), transformers -> {
                    entry.setItem((List) transformers);
                }).attach();
            });
            entry.getButton().setMessage(Text.translate("mcoo_build_with_ease.pattern_settings.presets", value.size()));
        });

        addWidget(Button.builder(getEntrance(), Text.translate("mcoo_build_with_ease.button.cancel"), button -> {
            detach();
        }).setBoundsGrid(getLeft(), getTop(), getWidth(), getHeight(), 0f, 0f, 0.5f).build());

        addWidget(Button.builder(getEntrance(), Text.translate("mcoo_build_with_ease.button.save"), button -> {
            consumer.accept(config);
            detachAll();
        }).setBoundsGrid(getLeft(), getTop(), getWidth(), getHeight(), 0f, 0.5f, 0.5f).build());

    }

    @Override
    protected McooBuildwithEaseClient getEntrance() {
        return (McooBuildwithEaseClient) super.getEntrance();
    }
}
