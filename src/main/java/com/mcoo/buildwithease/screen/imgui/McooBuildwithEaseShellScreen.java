package com.mcoo.buildwithease.screen.imgui;

import java.util.ArrayList;
import java.util.List;

import com.mcoo.buildwithease.McooBuildwithEaseClient;
import com.mcoo.buildwithease.screen.common.McooBuildwithEaseScreen;
import com.mcoo.buildwithease.screen.imgui.panel.ImGuiPanel;
import com.mcoo.buildwithease.screen.imgui.panel.RenderSettingsPanel;
import com.mcoo.buildwithease.screen.imgui.widget.ImGuiShell;
import com.mcoo.buildwithease.screen.imgui.widget.ImGuiWidgets;
import dev.huskuraft.universal.api.gui.ImGuiScreen;
import dev.huskuraft.universal.api.text.Text;

/**
 * ImGui 二级菜单的<b>总壳</b>：左栏选功能面板，中间是面板主体，右侧是当前对象的详情。
 *
 * <h3>职责边界</h3>
 * 这个类<b>只负责装配</b>：三栏布局、面板导航、异常隔离、关闭。
 * 具体内容全在 {@link ImGuiPanel} 实现里 —— 加一个新功能 = 加一个面板 + 在 {@link #buildPanels()}
 * 里注册一行，不用碰这个类。
 *
 * <h3>异常隔离</h3>
 * 每个面板的中间/右侧内容**分别** try/catch：一个面板画崩了只是那一栏空着，
 * 导航和别的面板照常可用，也不会把游戏带走（沿用渲染模块框架的隔离契约）。
 */
public class McooBuildwithEaseShellScreen extends McooBuildwithEaseScreen implements ImGuiScreen {

    private final List<ImGuiPanel> panels;
    private int selectedIndex;

    public McooBuildwithEaseShellScreen(McooBuildwithEaseClient entrance) {
        this(entrance, null);
    }

    /**
     * @param initialPanelId 打开时要选中的面板 id（{@code null} = 第一个）
     */
    public McooBuildwithEaseShellScreen(McooBuildwithEaseClient entrance, String initialPanelId) {
        super(entrance, Text.translate("mcoo_build_with_ease.settings.title"));
        this.panels = buildPanels();
        this.selectedIndex = indexOf(panels, initialPanelId);
    }

    /** 已注册的面板。新功能在这里加一行。 */
    private List<ImGuiPanel> buildPanels() {
        List<ImGuiPanel> list = new ArrayList<>();
        list.add(new RenderSettingsPanel(getEntrance()));
        return list;
    }

    private static int indexOf(List<ImGuiPanel> panels, String id) {
        if (id != null) {
            for (int i = 0; i < panels.size(); i++) {
                if (panels.get(i).id().equals(id)) {
                    return i;
                }
            }
        }
        return 0;
    }

    private ImGuiPanel selectedPanel() {
        if (panels.isEmpty()) {
            return null;
        }
        if (selectedIndex < 0 || selectedIndex >= panels.size()) {
            selectedIndex = 0;
        }
        return panels.get(selectedIndex);
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
                    this::drawNavigation,
                    this::drawContent,
                    this::drawDetails);
        } finally {
            ImGuiShell.end();
        }
    }

    // ------------------------------------------------------------------
    // 左栏：功能面板导航
    // ------------------------------------------------------------------

    private void drawNavigation() {
        ImGuiShell.panelTitle("mcoo_build_with_ease.shell.tools");
        for (int i = 0; i < panels.size(); i++) {
            var panel = panels.get(i);
            if (ImGuiShell.navItem(panel.titleKey(), i == selectedIndex)) {
                selectedIndex = i;
            }
        }

        ImGuiShell.bottomAlign();
        ImGuiWidgets.separator();
        if (ImGuiWidgets.button("mcoo_build_with_ease.button.done")) {
            detach();
        }
    }

    // ------------------------------------------------------------------
    // 中间 / 右栏：交给当前面板（各自独立兜底）
    // ------------------------------------------------------------------

    private void drawContent() {
        var panel = selectedPanel();
        if (panel == null) {
            ImGuiShell.contentPlaceholder("mcoo_build_with_ease.shell.preview_hint");
            return;
        }
        try {
            panel.renderContent();
        } catch (Throwable error) {
            // 只让这一栏空掉，导航和右侧照常
            ImGuiWidgets.diagnostic("[panel content failed] " + panel.id());
            ImGuiWidgets.diagnostic(String.valueOf(error));
        }
    }

    private void drawDetails() {
        var panel = selectedPanel();
        if (panel == null) {
            return;
        }
        try {
            panel.renderDetails();
        } catch (Throwable error) {
            ImGuiWidgets.diagnostic("[panel details failed] " + panel.id());
            ImGuiWidgets.diagnostic(String.valueOf(error));
        }
    }

}
