package com.mcoo.buildwithease.screen.builer;

import java.util.List;
import java.util.function.Consumer;

import com.mcoo.buildwithease.McooBuildwithEaseClient;
import dev.huskuraft.universal.api.gui.AbstractPanelScreen;
import dev.huskuraft.universal.api.gui.button.Button;
import dev.huskuraft.universal.api.gui.text.TextWidget;
import dev.huskuraft.universal.api.platform.Entrance;
import dev.huskuraft.universal.api.text.Text;
import com.mcoo.buildwithease.building.config.BuilderConfig;
import com.mcoo.buildwithease.building.config.ClientConfig;
import com.mcoo.buildwithease.building.config.PatternConfig;
import com.mcoo.buildwithease.building.config.RenderConfig;
import com.mcoo.buildwithease.screen.settings.SettingOptionsList;
import com.mcoo.buildwithease.screen.transformer.McooBuildwithEaseItemRandomizerPresetsScreen;

public class McooBuildwithEaseBuilderSettingsScreen extends AbstractPanelScreen {

    private final Consumer<ClientConfig> consumer;
    private ClientConfig originalConfig;
    private ClientConfig config;

    public McooBuildwithEaseBuilderSettingsScreen(Entrance entrance) {
        super(entrance, Text.translate("mcoo_build_with_ease.builder_settings.title"), PANEL_WIDTH_60, PANEL_HEIGHT_FULL);
        this.consumer = newConfig -> {
            getEntrance().getConfigStorage().update(config -> newConfig);
        };

        this.config = getEntrance().getConfigStorage().get();
        this.originalConfig = config;
    }

    @Override
    public void onCreate() {
        addWidget(new TextWidget(getEntrance(), getLeft() + getWidth() / 2, getTop() + PANEL_TITLE_HEIGHT_1 - 10, getScreenTitle().withColor(AbstractPanelScreen.TITLE_COLOR), TextWidget.Gravity.CENTER));

        var entries = addWidget(new SettingOptionsList(getEntrance(), getLeft() + PADDINGS_H, getTop() + PANEL_TITLE_HEIGHT_1, getWidth() - PADDINGS_H * 2 - 8, getHeight() - PANEL_TITLE_HEIGHT_1 - PANEL_BUTTON_ROW_HEIGHT_1, false, false));

        entries.addSwitchEntry(Text.translate("mcoo_build_with_ease.render_settings.show_block_preview"), null, this.config.renderConfig().showBlockPreview(), (value) -> {
            var renderConfig = this.config.renderConfig();
            this.config = this.config.withRenderConfig(new RenderConfig(value, renderConfig.showOtherPlayersBuild(), renderConfig.showOtherPlayersBuildTooltips(), renderConfig.maxRenderVolume(), renderConfig.maxRenderDistance()));
        });
        entries.addSwitchEntry(Text.translate("mcoo_build_with_ease.render_settings.show_other_players_build"), null, this.config.renderConfig().showOtherPlayersBuild(), (value) -> {
            var renderConfig = this.config.renderConfig();
            this.config = this.config.withRenderConfig(new RenderConfig(renderConfig.showBlockPreview(), value, renderConfig.showOtherPlayersBuildTooltips(), renderConfig.maxRenderVolume(), renderConfig.maxRenderDistance()));
        });
//        entries.addSwitchEntry(Text.translate("mcoo_build_with_ease.render_settings.show_other_players_build_tooltips"), null, config.showOtherPlayersBuildTooltips(), (value) -> {
//            this.config = new RenderConfig(config.showBlockPreview(), config.showOtherPlayersBuild(), value, config.maxRenderVolume(), config.maxRenderDistance());
//        });
        entries.addIntegerEntry(Text.translate("mcoo_build_with_ease.render_settings.max_render_volume"), null, this.config.renderConfig().maxRenderVolume(), RenderConfig.MAX_RENDER_VOLUME_MIN, RenderConfig.MAX_RENDER_VOLUME_MAX, (value) -> {
            var renderConfig = this.config.renderConfig();
            this.config = this.config.withRenderConfig(new RenderConfig(renderConfig.showBlockPreview(), renderConfig.showOtherPlayersBuild(), renderConfig.showOtherPlayersBuildTooltips(), value, renderConfig.maxRenderDistance()));
        });
//        entries.addIntegerEntry(Text.translate("mcoo_build_with_ease.render_settings.max_render_distance"), null, config.maxRenderDistance(), RenderConfig.MIN_MAX_RENDER_DISTANCE, RenderConfig.MAX_MAX_RENDER_DISTANCE, (value) -> {
//            this.config = new RenderConfig(config.showOtherPlayersBuild(), config.showOtherPlayersBuildTooltips(), config.showBlockPreview(), config.maxRenderVolume(), value);
//        });


        entries.addIntegerEntry(Text.translate("mcoo_build_with_ease.builder_settings.reserved_tool_durability"), null, this.config.builderConfig().reservedToolDurability(), BuilderConfig.RESERVED_TOOL_DURABILITY_RANGE.min(), BuilderConfig.RESERVED_TOOL_DURABILITY_RANGE.max(), (value) -> {
            this.config = this.config.withBuilderConfig(new BuilderConfig(value, this.config.builderConfig().passiveMode()));
        });
        entries.addSwitchEntry(Text.translate("mcoo_build_with_ease.builder_settings.passive_mode"), null, this.config.builderConfig().passiveMode(), (value) -> {
            this.config = this.config.withBuilderConfig(new BuilderConfig(config.builderConfig().reservedToolDurability(), value));
        });

        entries.addTab(Text.translate("mcoo_build_with_ease.pattern_settings.item_randomizer_presets"), null, config.patternConfig().transformerPreset(), (value) -> {
            this.config = this.config.withPatternConfig(new PatternConfig(value));
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

    public ClientConfig getConfig() {
        return config;
    }

    @Override
    public void onReload() {


    }

    @Override
    protected McooBuildwithEaseClient getEntrance() {
        return (McooBuildwithEaseClient) super.getEntrance();
    }
}
