package com.mcoo.buildwithease.screen.settings;

import java.util.ArrayList;

import com.mcoo.buildwithease.McooBuildwithEaseClient;
import com.mcoo.buildwithease.McooBuildwithEaseClientConfigStorage;
import com.mcoo.buildwithease.McooBuildwithEaseConfigStorage;
import dev.huskuraft.universal.api.gui.AbstractPanelScreen;
import dev.huskuraft.universal.api.gui.button.Button;
import dev.huskuraft.universal.api.gui.text.TextWidget;
import dev.huskuraft.universal.api.gui.tooltip.TooltipHelper;
import dev.huskuraft.universal.api.platform.Entrance;
import dev.huskuraft.universal.api.text.ChatFormatting;
import dev.huskuraft.universal.api.text.Text;
import com.mcoo.buildwithease.networking.packets.player.PlayerPermissionCheckPacket;
import com.mcoo.buildwithease.screen.builer.McooBuildwithEaseBuilderSettingsScreen;
import com.mcoo.buildwithease.screen.general.McooBuildwithEaseGlobalGeneralSettingsScreen;

public class McooBuildwithEaseSettingsScreen extends AbstractPanelScreen {

    public McooBuildwithEaseSettingsScreen(Entrance entrance) {
        super(entrance, Text.translate("mcoo_build_with_ease.settings.title"), PANEL_WIDTH_42, PANEL_TITLE_HEIGHT_1 + PANEL_BUTTON_ROW_HEIGHT_3);
    }

    private Button constraintButton;
    private Button buildertButton;
//    private Button patternButton;
//    private Button clipboardButton;
//    private Button renderButton;

    @Override
    protected McooBuildwithEaseClient getEntrance() {
        return (McooBuildwithEaseClient) super.getEntrance();
    }

    @Override
    public void onCreate() {
        addWidget(new TextWidget(getEntrance(), getLeft() + getWidth() / 2, getTop() + PANEL_TITLE_HEIGHT_1 - 10, getScreenTitle().withColor(AbstractPanelScreen.TITLE_COLOR), TextWidget.Gravity.CENTER));

        this.constraintButton = addWidget(Button.builder(getEntrance(), Text.translate("mcoo_build_with_ease.general_settings.title"), button -> {
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
        }).setBoundsGrid(getLeft(), getTop(), getWidth(), getHeight(), 2f   , 0f, 1f).build());
        this.buildertButton = addWidget(Button.builder(getEntrance(), Text.translate("mcoo_build_with_ease.builder_settings.title"), button -> {
            new McooBuildwithEaseBuilderSettingsScreen(getEntrance()).attach();
        }).setBoundsGrid(getLeft(), getTop(), getWidth(), getHeight(), 1f, 0f, 1f).build());
//        this.renderButton = addWidget(Button.builder(getEntrance(), Text.translate("mcoo_build_with_ease.render_settings.title"), button -> {
//            new McooBuildwithEaseRenderSettingsScreen(getEntrance()).attach();
//        }).setBoundsGrid(getLeft(), getTop(), getWidth(), getHeight(), 1f, 0f, 1f).build());
//        this.patterntButton = addWidget(Button.builder(getEntrance(), Text.translate("mcoo_build_with_ease.pattern_settings.title"), button -> {
//            new McooBuildwithEasePatternSettingsScreen(getEntrance()).attach();
//        }).setBoundsGrid(getLeft(), getTop(), getWidth(), getHeight(), 2f, 0f, 1f).build());
//        this.clipboardButton = addWidget(Button.builder(getEntrance(), Text.translate("mcoo_build_with_ease.clipboard_settings.title"), button -> {
//            new McooBuildwithEaseClipboardSettingsScreen(getEntrance()).attach();
//        }).setBoundsGrid(getLeft(), getTop(), getWidth(), getHeight(), 1f, 0f, 1f).build());

        addWidget(Button.builder(getEntrance(), Text.translate("mcoo_build_with_ease.button.done"), button -> {
            detach();
        }).setBoundsGrid(getLeft(), getTop(), getWidth(), getHeight(), 0f, 0f, 1f).build());

    }

    @Override
    public void onReload() {

        var constraintTooltip = new ArrayList<Text>();
        constraintTooltip.add(Text.translate("mcoo_build_with_ease.general_settings.title").withStyle(ChatFormatting.WHITE));
        constraintTooltip.add(TooltipHelper.holdShiftForSummary());
        if (TooltipHelper.isSummaryButtonDown()) {
            constraintTooltip.add(Text.empty());
            constraintTooltip.addAll(
                    TooltipHelper.wrapLines(getTypeface(), Text.translate("mcoo_build_with_ease.general_settings.tooltip", Text.text("[%s]".formatted(McooBuildwithEaseConfigStorage.CONFIG_NAME)).withStyle(ChatFormatting.GOLD)).withStyle(ChatFormatting.GRAY))
            );
        }
        this.constraintButton.setTooltip(constraintTooltip);

        var builderTooltip = new ArrayList<Text>();
        builderTooltip.add(Text.translate("mcoo_build_with_ease.builder_settings.title").withStyle(ChatFormatting.WHITE));
        builderTooltip.add(TooltipHelper.holdShiftForSummary());
        if (TooltipHelper.isSummaryButtonDown()) {
            builderTooltip.add(Text.empty());
            builderTooltip.addAll(
                    TooltipHelper.wrapLines(getTypeface(), Text.translate("mcoo_build_with_ease.builder_settings.tooltip", Text.text("[%s]".formatted(McooBuildwithEaseClientConfigStorage.CONFIG_NAME)).withStyle(ChatFormatting.GOLD)).withStyle(ChatFormatting.GRAY))
            );
        }
        this.buildertButton.setTooltip(builderTooltip);

//        var patternTooltip = new ArrayList<Text>();
//        patternTooltip.add(Text.translate("mcoo_build_with_ease.pattern_settings.title").withStyle(ChatFormatting.WHITE));
//        patternTooltip.add(TooltipHelper.holdShiftForSummary());
//        if (TooltipHelper.isSummaryButtonDown()) {
//            patternTooltip.add(Text.empty());
//            patternTooltip.addAll(
//                    TooltipHelper.wrapLines(getTypeface(), Text.translate("mcoo_build_with_ease.pattern_settings.tooltip", Text.text("[%s]".formatted(McooBuildwithEaseClientConfigStorage.CONFIG_NAME)).withStyle(ChatFormatting.GOLD)).withStyle(ChatFormatting.GRAY))
//            );
//        }
//        this.patterntButton.setTooltip(patternTooltip);
//
//        var renderTooltip = new ArrayList<Text>();
//        renderTooltip.add(Text.translate("mcoo_build_with_ease.render_settings.title").withStyle(ChatFormatting.WHITE));
//        renderTooltip.add(TooltipHelper.holdShiftForSummary());
//        if (TooltipHelper.isSummaryButtonDown()) {
//            renderTooltip.add(Text.empty());
//            renderTooltip.addAll(
//                    TooltipHelper.wrapLines(getTypeface(), Text.translate("mcoo_build_with_ease.render_settings.tooltip", Text.text("[%s]".formatted(McooBuildwithEaseClientConfigStorage.CONFIG_NAME)).withStyle(ChatFormatting.GOLD)).withStyle(ChatFormatting.GRAY))
//            );
//        }
//        this.renderButton.setTooltip(renderTooltip);
    }
}
