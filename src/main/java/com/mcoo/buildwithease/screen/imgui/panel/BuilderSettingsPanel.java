package com.mcoo.buildwithease.screen.imgui.panel;

import com.mcoo.buildwithease.McooBuildwithEaseClient;
import com.mcoo.buildwithease.building.config.BuilderConfig;
import com.mcoo.buildwithease.building.config.RenderConfig;
import com.mcoo.buildwithease.screen.imgui.widget.ImGuiShell;
import com.mcoo.buildwithease.screen.imgui.widget.ImGuiWidgets;

/**
 * 建造设置面板（客户端行为）。
 *
 * <h3>为什么是「一屏表单」而不是「列表 + 详情」</h3>
 * 剪贴板、图案有「很多个对象」可选，所以用左列表右详情；
 * 建造设置只有一组固定开关，<b>没有可选项</b> —— 硬套两栏只会多一层没意义的选择。
 * 这一栏就是纯表单，右栏留给「这一项是干什么的」说明。
 *
 * <p>所有值都写回 {@code ConfigStorage}（客户端配置），和自研界面共用同一份配置。
 */
public final class BuilderSettingsPanel implements ImGuiPanel {

    private static final String ID = "builder";

    private final McooBuildwithEaseClient entrance;

    public BuilderSettingsPanel(McooBuildwithEaseClient entrance) {
        this.entrance = entrance;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String titleKey() {
        return "mcoo_build_with_ease.builder_settings.title";
    }

    // ------------------------------------------------------------------
    // 中间栏：表单
    // ------------------------------------------------------------------

    @Override
    public void renderContent() {
        var config = entrance.getConfigStorage().get();
        var builder = config.builderConfig();
        var render = config.renderConfig();

        ImGuiWidgets.diagnostic("建造行为");
        ImGuiWidgets.separator();

        int durability = ImGuiShell.fieldSliderInt("mcoo_build_with_ease.builder_settings.reserved_tool_durability",
                builder.reservedToolDurability(),
                BuilderConfig.RESERVED_TOOL_DURABILITY_RANGE.min(),
                BuilderConfig.RESERVED_TOOL_DURABILITY_RANGE.max());
        if (durability != builder.reservedToolDurability()) {
            save(config.withBuilderConfig(builder.withReservedToolDurability(durability)), config.renderConfig());
        }

        boolean passive = ImGuiShell.fieldCheckbox("mcoo_build_with_ease.builder_settings.passive_mode",
                builder.passiveMode());
        if (passive != builder.passiveMode()) {
            save(config.withBuilderConfig(builder.withPassiveMode(passive)), config.renderConfig());
        }

        ImGuiWidgets.separator();
        ImGuiWidgets.diagnostic("预览与渲染");
        ImGuiWidgets.separator();

        boolean blockPreview = ImGuiShell.fieldCheckbox("mcoo_build_with_ease.render_settings.show_block_preview",
                render.showBlockPreview());
        if (blockPreview != render.showBlockPreview()) {
            save(config.withBuilderConfig(builder), withShowBlockPreview(render, blockPreview));
        }

        boolean otherPlayers = ImGuiShell.fieldCheckbox("mcoo_build_with_ease.render_settings.show_other_players_build",
                render.showOtherPlayersBuild());
        if (otherPlayers != render.showOtherPlayersBuild()) {
            save(config.withBuilderConfig(builder), withShowOtherPlayersBuild(render, otherPlayers));
        }

        int maxVolume = ImGuiShell.fieldSliderInt("mcoo_build_with_ease.render_settings.max_render_volume",
                render.maxRenderVolume(),
                RenderConfig.MAX_RENDER_VOLUME_MIN,
                RenderConfig.MAX_RENDER_VOLUME_MAX);
        if (maxVolume != render.maxRenderVolume()) {
            save(config.withBuilderConfig(builder), withMaxRenderVolume(render, maxVolume));
        }
    }

    // ------------------------------------------------------------------
    // 右栏：说明
    // ------------------------------------------------------------------

    @Override
    public void renderDetails() {
        ImGuiShell.panelTitle("mcoo_build_with_ease.shell.details");
        ImGuiWidgets.text("mcoo_build_with_ease.builder_settings.help");
        ImGuiWidgets.separator();
        ImGuiWidgets.diagnostic("这些设置只影响本机客户端，不改变服务端行为。");
    }

    // ------------------------------------------------------------------
    // 保存（RenderConfig 是 record，逐个字段重建）
    // ------------------------------------------------------------------

    private void save(com.mcoo.buildwithease.building.config.ClientConfig builderUpdated,
                      RenderConfig render) {
        entrance.getConfigStorage().update(config -> builderUpdated.withRenderConfig(render));
    }

    private static RenderConfig withShowBlockPreview(RenderConfig c, boolean value) {
        return new RenderConfig(value, c.showOtherPlayersBuild(), c.showOtherPlayersBuildTooltips(),
                c.maxRenderVolume(), c.maxRenderDistance());
    }

    private static RenderConfig withShowOtherPlayersBuild(RenderConfig c, boolean value) {
        return new RenderConfig(c.showBlockPreview(), value, c.showOtherPlayersBuildTooltips(),
                c.maxRenderVolume(), c.maxRenderDistance());
    }

    private static RenderConfig withMaxRenderVolume(RenderConfig c, int value) {
        return new RenderConfig(c.showBlockPreview(), c.showOtherPlayersBuild(),
                c.showOtherPlayersBuildTooltips(), value, c.maxRenderDistance());
    }

}
