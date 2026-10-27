package com.mcoo.buildwithease.screen.imgui;

import com.mcoo.buildwithease.McooBuildwithEaseClient;
import com.mcoo.buildwithease.screen.common.McooBuildwithEaseScreen;
import com.mcoo.buildwithease.screen.imgui.widget.ImGuiShell;
import com.mcoo.buildwithease.screen.imgui.widget.ImGuiWidgets;
import dev.huskuraft.universal.api.gui.ImGuiScreen;
import dev.huskuraft.universal.api.text.Text;

/**
 * Shell 排布原型（<b>只有假数据，用来定观感和分栏比例</b>）。
 *
 * <p>目的：在把二十来个界面一个个搬进 ImGui 之前，先把「长什么样」定下来。
 * 用户要求参考 Axiom —— 类似 Photoshop 的「左侧工具列表 + 右侧工具详情」。
 * 所以这里只画骨架：左栏导航、中间内容占位、右栏可折叠的详情分组。
 *
 * <p>调出来看：{@code -Dmcoo.debugScreen=shell}（或 {@code gui-verify.ps1 -Screens shell}）。
 *
 * <p>⚠️ 这是原型的<b>唯一</b>用途。真界面接手时，把 {@link #drawTools} / {@link #drawDetails}
 * 里的假条目换成真实数据源，或者按同样结构另写面板类，然后删掉本类。
 */
public class McooBuildwithEaseShellPreviewScreen extends McooBuildwithEaseScreen implements ImGuiScreen {

    /** 原型里当前选中的左栏条目（只为演示选中态）。 */
    private int selected = 0;

    public McooBuildwithEaseShellPreviewScreen(McooBuildwithEaseClient entrance) {
        super(entrance, Text.translate("mcoo_build_with_ease.shell.tools"));
    }

    @Override
    public void renderImGui() {
        boolean open = ImGuiShell.begin();
        try {
            if (!open) {
                return;
            }
            ImGuiShell.layout(
                    ImGuiShell.suggestedLeftWidth(),
                    ImGuiShell.suggestedRightWidth(),
                    this::drawTools,
                    this::drawContent,
                    this::drawDetails);
        } finally {
            ImGuiShell.end();
        }
    }

    // ------------------------------------------------------------------
    // 左栏：工具 / 分类列表
    // ------------------------------------------------------------------

    private void drawTools() {
        navIndex = 0;
        ImGuiShell.panelTitle("mcoo_build_with_ease.shell.tools");

        ImGuiShell.section("mcoo_build_with_ease.shell.section_build");
        nav("mcoo_build_with_ease.builder_settings.title");
        nav("mcoo_build_with_ease.pattern_settings.title");
        nav("mcoo_build_with_ease.transformer_presets.title");

        ImGuiShell.section("mcoo_build_with_ease.shell.section_clipboard");
        nav("mcoo_build_with_ease.clipboard_settings.title");

        ImGuiShell.section("mcoo_build_with_ease.shell.section_settings");
        nav("mcoo_build_with_ease.general_settings.title");
        nav("mcoo_build_with_ease.render_settings.title");
        nav("mcoo_build_with_ease.settings.title");
    }

    private void nav(String labelKey) {
        if (ImGuiShell.navItem(labelKey, selected == navIndex)) {
            selected = navIndex;
        }
        navIndex++;
    }

    /** 只是给上面 nav() 计数用 —— 每次绘制从 0 开始数。 */
    private int navIndex;

    // ------------------------------------------------------------------
    // 中间：内容区（真界面里是 3D 预览 / 列表）
    // ------------------------------------------------------------------

    private void drawContent() {
        ImGuiShell.contentPlaceholder("mcoo_build_with_ease.shell.preview_hint");
    }

    // ------------------------------------------------------------------
    // 右栏：当前工具的详情
    // ------------------------------------------------------------------

    private void drawDetails() {
        ImGuiShell.panelTitle("mcoo_build_with_ease.shell.details");

        if (ImGuiShell.collapsible("mcoo_build_with_ease.shell.section_build", true)) {
            ImGuiShell.fieldSliderInt("mcoo_build_with_ease.transformer.array.count", 4, 1, 16);
            ImGuiShell.fieldSliderInt("mcoo_build_with_ease.transformer.radial.slices", 8, 3, 24);
            ImGuiShell.fieldCheckbox("mcoo_build_with_ease.render.enable_module", true);
        }

        if (ImGuiShell.collapsible("mcoo_build_with_ease.transformer_presets.title", false)) {
            ImGuiShell.fieldText("mcoo_build_with_ease.transformer.array.offset.x", "0");
            ImGuiShell.fieldText("mcoo_build_with_ease.transformer.array.offset.y", "1");
            ImGuiShell.fieldText("mcoo_build_with_ease.transformer.array.offset.z", "0");
        }

        // 按钮压到面板底边（真界面里「应用 / 取消」这个位置是固定的）
        ImGuiShell.bottomAlign();
        ImGuiWidgets.separator();
        if (ImGuiWidgets.button("mcoo_build_with_ease.button.done")) {
            detach();
        }
    }

    @Override
    public void onCreate() {
        navIndex = 0;
    }

}
