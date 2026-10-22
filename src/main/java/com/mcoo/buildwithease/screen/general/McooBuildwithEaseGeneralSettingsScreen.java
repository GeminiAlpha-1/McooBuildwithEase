package com.mcoo.buildwithease.screen.general;

import java.util.ArrayList;

import com.mcoo.buildwithease.McooBuildwithEaseClient;
import com.mcoo.buildwithease.McooBuildwithEaseConfigStorage;
import dev.huskuraft.universal.api.gui.AbstractPanelScreen;
import dev.huskuraft.universal.api.gui.button.Button;
import dev.huskuraft.universal.api.gui.text.TextWidget;
import dev.huskuraft.universal.api.gui.tooltip.TooltipHelper;
import dev.huskuraft.universal.api.platform.Entrance;
import dev.huskuraft.universal.api.text.ChatFormatting;
import dev.huskuraft.universal.api.text.Text;
import com.mcoo.buildwithease.networking.packets.player.PlayerPermissionCheckPacket;
import com.mcoo.buildwithease.screen.settings.McooBuildwithEaseNotAnOperatorScreen;
import com.mcoo.buildwithease.screen.settings.McooBuildwithEaseSessionStatusScreen;

public class McooBuildwithEaseGeneralSettingsScreen extends AbstractPanelScreen {

    public McooBuildwithEaseGeneralSettingsScreen(Entrance entrance) {
        super(entrance, Text.translate("mcoo_build_with_ease.general_settings.title"), PANEL_WIDTH_42, PANEL_TITLE_HEIGHT_1 + PANEL_BUTTON_ROW_HEIGHT_3);
    }

    private Button globalButton;
    private Button playerButton;

    @Override
    public void onCreate() {
        addWidget(new TextWidget(getEntrance(), getLeft() + getWidth() / 2, getTop() + PANEL_TITLE_HEIGHT_1 - 10, getScreenTitle().withColor(AbstractPanelScreen.TITLE_COLOR), TextWidget.Gravity.CENTER));

        this.globalButton = addWidget(Button.builder(getEntrance(), Text.translate("mcoo_build_with_ease.global_general_settings.title"), button -> {
            com.mcoo.buildwithease.McooBuildwithEase.LOGGER.info("[general] 全局通用设置 被点击, 会话状态 = {}", getEntrance().getSessionManager().getSessionStatus());
            if (!getEntrance().getSessionManager().isSessionValid()) {
                getEntrance().getClient().execute(() -> {
                    new McooBuildwithEaseSessionStatusScreen(getEntrance()).attach();
                });
            } else {
                getEntrance().getChannel().sendPacket(new PlayerPermissionCheckPacket(getEntrance().getClient().getPlayer().getId()), (packet) -> {
                    if (packet.granted()) {
                        getEntrance().getClient().execute(() -> {
                            new McooBuildwithEaseGlobalGeneralSettingsScreen(getEntrance(), getEntrance().getSessionManager().getServerSessionConfigOrEmpty().getGlobalConfig(), config -> {
                                getEntrance().getSessionManager().updateGlobalConfig(config);
                            }).attach();
                        });
                    } else {
                        getEntrance().getClient().execute(() -> {
                            new McooBuildwithEaseNotAnOperatorScreen(getEntrance()).attach();
                        });
                    }
                });
            }

        }).setBoundsGrid(getLeft(), getTop(), getWidth(), getHeight(), 2f, 0f, 1f).build());
        this.playerButton = addWidget(Button.builder(getEntrance(), Text.translate("mcoo_build_with_ease.player_general_settings.title"), button -> {
            getEntrance().getChannel().sendPacket(new PlayerPermissionCheckPacket(getEntrance().getClient().getPlayer().getId()), (packet) -> {

                if (!getEntrance().getSessionManager().isSessionValid()) {
                    getEntrance().getClient().execute(() -> {
                        new McooBuildwithEaseSessionStatusScreen(getEntrance()).attach();
                    });
                } else {
                    if (packet.granted()) {
                        getEntrance().getClient().execute(() -> {
                            new McooBuildwithEasePlayerGeneralSettingsListScreen(getEntrance(), getEntrance().getSessionManager().getServerSessionConfigOrEmpty().playerConfigs(), playerConfigs -> {
                                getEntrance().getSessionManager().updatePlayerConfig(playerConfigs);
                            }).attach();
                        });
                    } else {
                        getEntrance().getClient().execute(() -> {
                            new McooBuildwithEaseNotAnOperatorScreen(getEntrance()).attach();
                        });
                    }
                }
            });
        }).setBoundsGrid(getLeft(), getTop(), getWidth(), getHeight(), 1f, 0f, 1f).build());

        addWidget(Button.builder(getEntrance(), Text.translate("mcoo_build_with_ease.button.done"), button -> {
            detach();
        }).setBoundsGrid(getLeft(), getTop(), getWidth(), getHeight(), 0f, 0f, 1f).build());

    }

    @Override
    protected McooBuildwithEaseClient getEntrance() {
        return (McooBuildwithEaseClient) super.getEntrance();
    }

    @Override
    public void onReload() {
        var globalTooltip = new ArrayList<Text>();
        globalTooltip.add(Text.translate("mcoo_build_with_ease.global_general_settings.title").withStyle(ChatFormatting.WHITE));
        globalTooltip.add(TooltipHelper.holdShiftForSummary());
        if (TooltipHelper.isSummaryButtonDown()) {
            globalTooltip.add(Text.empty());
            globalTooltip.addAll(
                    TooltipHelper.wrapLines(getTypeface(), Text.translate("mcoo_build_with_ease.global_general_settings.tooltip", Text.text("[%s]".formatted(McooBuildwithEaseConfigStorage.CONFIG_NAME)).withStyle(ChatFormatting.GOLD)).withStyle(ChatFormatting.GRAY))
            );
        }
        this.globalButton.setTooltip(globalTooltip);


        var playerTooltip = new ArrayList<Text>();
        playerTooltip.add(Text.translate("mcoo_build_with_ease.player_general_settings.title").withStyle(ChatFormatting.WHITE));
        playerTooltip.add(TooltipHelper.holdShiftForSummary());
        if (TooltipHelper.isSummaryButtonDown()) {
            playerTooltip.add(Text.empty());
            playerTooltip.addAll(
                    TooltipHelper.wrapLines(getTypeface(), Text.translate("mcoo_build_with_ease.player_general_settings.tooltip", Text.text("[%s]".formatted(McooBuildwithEaseConfigStorage.CONFIG_NAME)).withStyle(ChatFormatting.GOLD)).withStyle(ChatFormatting.GRAY))
            );
        }
        this.playerButton.setTooltip(playerTooltip);
    }
}
