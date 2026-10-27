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
        super(entrance, Text.translate("mcoo_build_with_ease.settings.title"), PANEL_WIDTH_42, PANEL_TITLE_HEIGHT_1 + PANEL_BUTTON_ROW_HEIGHT_4 + PANEL_BUTTON_ROW_HEIGHT_3);
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
        // 渲染设置已并入 ImGui Shell（三栏：左导航 / 中内容 / 右详情）。
        // 没装 ImGui 库就不显示这个入口 —— 二级菜单整体隐藏，不崩。
        if (dev.huskuraft.universal.api.gui.ImGuiSupport.isAvailable()) {
            addWidget(Button.builder(getEntrance(), Text.translate("mcoo_build_with_ease.render_settings.title"), button -> {
                new com.mcoo.buildwithease.screen.imgui.McooBuildwithEaseShellScreen(getEntrance(), "render").attach();
            }).setBoundsGrid(getLeft(), getTop(), getWidth(), getHeight(), 3f, 0f, 1f).build());
        }
//        this.patterntButton = addWidget(Button.builder(getEntrance(), Text.translate("mcoo_build_with_ease.pattern_settings.title"), button -> {
//            new McooBuildwithEasePatternSettingsScreen(getEntrance()).attach();
//        }).setBoundsGrid(getLeft(), getTop(), getWidth(), getHeight(), 2f, 0f, 1f).build());
//        this.clipboardButton = addWidget(Button.builder(getEntrance(), Text.translate("mcoo_build_with_ease.clipboard_settings.title"), button -> {
//            new McooBuildwithEaseClipboardSettingsScreen(getEntrance()).attach();
//        }).setBoundsGrid(getLeft(), getTop(), getWidth(), getHeight(), 1f, 0f, 1f).build());

        // 图案 / 剪贴板 / 玩家 三个入口先进 ImGui Shell 的对应面板。
        // 【setBoundsGrid 的参数】第 5 个是**行号（从底往上数，0 = 最底一行）**，
        // 第 6 个是列偏移，第 7 个是跨列数（>1 会横向占满，写成 2 就会得到一条横穿的怪按钮 —— 踩过）。
        addWidget(Button.builder(getEntrance(), Text.translate("mcoo_build_with_ease.pattern.title"), button -> {
            new com.mcoo.buildwithease.screen.imgui.McooBuildwithEaseShellScreen(getEntrance(), "pattern").attach();
        }).setBoundsGrid(getLeft(), getTop(), getWidth(), getHeight(), 4f, 0f, 1f).build());
        addWidget(Button.builder(getEntrance(), Text.translate("mcoo_build_with_ease.clipboard.edit.title"), button -> {
            new com.mcoo.buildwithease.screen.imgui.McooBuildwithEaseShellScreen(getEntrance(), "clipboard").attach();
        }).setBoundsGrid(getLeft(), getTop(), getWidth(), getHeight(), 5f, 0f, 1f).build());
        addWidget(Button.builder(getEntrance(), Text.translate("mcoo_build_with_ease.player_list.title"), button -> {
            new com.mcoo.buildwithease.screen.imgui.McooBuildwithEaseShellScreen(getEntrance(), "players").attach();
        }).setBoundsGrid(getLeft(), getTop(), getWidth(), getHeight(), 6f, 0f, 1f).build());
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
