package com.mcoo.buildwithease.screen.test;

import java.util.logging.Logger;

import com.mcoo.buildwithease.McooBuildwithEaseClient;
import dev.huskuraft.universal.api.gui.AbstractPanelScreen;
import dev.huskuraft.universal.api.gui.button.Button;
import dev.huskuraft.universal.api.gui.container.SimpleEntryList;
import dev.huskuraft.universal.api.gui.input.EditBox;
import dev.huskuraft.universal.api.gui.text.TextWidget;
import dev.huskuraft.universal.api.platform.Entrance;
import dev.huskuraft.universal.api.text.Text;
import com.mcoo.buildwithease.building.config.ClientConfig;
import com.mcoo.buildwithease.building.config.ClipboardConfig;
import com.mcoo.buildwithease.building.config.PatternConfig;
import com.mcoo.buildwithease.building.config.RenderConfig;
import com.mcoo.buildwithease.screen.general.McooBuildwithEaseGeneralSettingsScreen;
import com.mcoo.buildwithease.screen.pattern.McooBuildwithEasePatternScreen;
import com.mcoo.buildwithease.screen.player.McooBuildwithEaseOnlinePlayersScreen;
import com.mcoo.buildwithease.screen.settings.McooBuildwithEaseSettingsScreen;

public class McooBuildwithEaseTestScreen extends AbstractPanelScreen {

    public McooBuildwithEaseTestScreen(Entrance entrance) {
        super(entrance, Text.text("Test"), PANEL_WIDTH_60, PANEL_HEIGHT_FULL);
    }

    @Override
    public void onCreate() {

        addWidget(new TextWidget(getEntrance(), getLeft() + getWidth() / 2, getTop() + PANEL_TITLE_HEIGHT_1 - 10, getScreenTitle().withColor(AbstractPanelScreen.TITLE_COLOR), TextWidget.Gravity.CENTER));

        addWidget(Button.builder(getEntrance(), Text.translate("mcoo_build_with_ease.test.cancel"), button -> {
            detach();
        }).setBoundsGrid(getLeft(), getTop(), getWidth(), getHeight(), 0f, 0f, 1f).build());

        var entries = addWidget(new SimpleEntryList(getEntrance(), getLeft() + PADDINGS_H, getTop() + PANEL_TITLE_HEIGHT_1, getWidth() - PADDINGS_H * 2, getHeight() - PANEL_TITLE_HEIGHT_1 - PANEL_BUTTON_ROW_HEIGHT_1));

        entries.addSimpleEntry(entry -> {
            var editBox = entry.addWidget(new EditBox(getEntrance(), entry.getLeft(), entry.getY(), entry.getWidth() - 64, 20, Text.empty()));
            entry.addWidget(new Button(getEntrance(), entry.getRight() - 64, entry.getTop(), 64, 20, Text.text("Execute"), button -> {
                getEntrance().getClient().sendCommand(editBox.getValue());
            }));
        });
        entries.addSimpleEntry(entry -> {
            entry.addWidget(new Button(getEntrance(), entry.getLeft(), entry.getTop(), entry.getWidth() / 2, 20, Text.text("Load Toml Config"), button -> {
                Logger.getAnonymousLogger().info("" + getEntrance().getConfigStorage().get());
            }));
            entry.addWidget(new Button(getEntrance(), entry.getLeft() + entry.getWidth() / 2, entry.getTop(), entry.getWidth() / 2, 20, Text.text("Save Toml Config"), button -> {
                getEntrance().getConfigStorage().set(
                        new ClientConfig(
                                new RenderConfig(),
                                new PatternConfig(),
                                new ClipboardConfig()
                        )
                );
            }));
        });
        entries.addSimpleEntry(entry -> {
            entry.addWidget(new Button(getEntrance(), entry.getLeft(), entry.getTop(), entry.getWidth(), 20, Text.text("Open McooBuildwithEaseSettingsScreen"), button -> {
                new McooBuildwithEaseSettingsScreen(getEntrance()).attach();
            }));
            entry.addWidget(new Button(getEntrance(), entry.getLeft(), entry.getTop() + 20, entry.getWidth(), 20, Text.text("Open McooBuildwithEaseConstraintSettingsScreen"), button -> {
                new McooBuildwithEaseGeneralSettingsScreen(getEntrance()).attach();
            }));
            entry.addWidget(new Button(getEntrance(), entry.getLeft(), entry.getTop() + 80, entry.getWidth(), 20, Text.text("Open McooBuildwithEaseOnlinePlayersScreen"), button -> {
                new McooBuildwithEaseOnlinePlayersScreen(getEntrance(), playerInfo -> {

                }).attach();
            }));
        });
        entries.addSimpleEntry(entry -> {
            entry.addWidget(new Button(getEntrance(), entry.getLeft(), entry.getTop(), entry.getWidth(), 20, Text.text("Open McooBuildwithEasePatternScreen"), button -> {
                new McooBuildwithEasePatternScreen(getEntrance()).attach();
            }));
        });
    }

    @Override
    protected McooBuildwithEaseClient getEntrance() {
        return (McooBuildwithEaseClient) super.getEntrance();
    }
}
