package com.mcoo.buildwithease.screen.imgui.panel;

import com.mcoo.buildwithease.McooBuildwithEaseClient;
import com.mcoo.buildwithease.screen.imgui.widget.ImGuiParameterBinder;
import com.mcoo.buildwithease.screen.imgui.widget.ImGuiShell;
import com.mcoo.buildwithease.screen.imgui.widget.ImGuiWidgets;
import dev.huskuraft.universal.api.text.Text;

/**
 * 渲染设置面板（渲染模块框架的展示窗口）。
 *
 * <p>布局：<b>中间</b>是 13 个模块的列表（点一行切到该模块）；<b>右侧</b>是选中模块的开关与参数。
 * 参数由模块自己声明（{@code RenderParameter}），这里只负责画 —— 以后加模块 / 加参数，
 * 这个类一行都不用改。
 *
 * <p>这个面板是 Shell 的「样板」：后面的建造 / 图案 / 剪贴板面板都按同样的结构写。
 */
public final class RenderSettingsPanel implements ImGuiPanel {

    private static final String ID = "render";

    private final McooBuildwithEaseClient entrance;

    /** 当前选中的模块 id（null = 还没选，默认选第一个）。 */
    private String selectedModuleId;

    public RenderSettingsPanel(McooBuildwithEaseClient entrance) {
        this.entrance = entrance;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String titleKey() {
        return "mcoo_build_with_ease.render_settings.title";
    }

    // ------------------------------------------------------------------
    // 中间栏：总开关 + 模块列表
    // ------------------------------------------------------------------

    @Override
    public void renderContent() {
        var host = entrance.getRenderManager().host();

        // 总开关放最上面：一屏里最先要看到的就是"自建渲染器到底开没开"
        var master = host.isMasterEnabled();
        if (ImGuiWidgets.checkbox("mcoo_build_with_ease.render.master_enable", master)) {
            host.setMasterEnabled(!master);
            save();
        }

        // 哪个模块挂了：用户最关心的失败信息，放列表上面
        var failures = host.failures();
        if (!failures.isEmpty()) {
            ImGuiWidgets.separator();
            for (var entry : failures.entrySet()) {
                ImGuiWidgets.textWrapped("mcoo_build_with_ease.render.module_failed");
                ImGuiWidgets.diagnostic("  " + entry.getKey() + ": " + entry.getValue());
            }
        }

        ImGuiWidgets.separator();

        var modules = host.modules();
        if (modules.isEmpty()) {
            ImGuiWidgets.diagnostic("(no render modules registered)");
            return;
        }
        if (selectedModuleId == null) {
            selectedModuleId = modules.get(0).id();
        }

        for (var module : modules) {
            // 行内左侧一个勾表示该模块开着 —— 比"只能点进右侧看"直观
            boolean enabled = host.isModuleEnabled(module.id());
            String label = Text.translate(module.nameKey()).getString();
            if (ImGuiShell.navItemRaw((enabled ? "● " : "○ ") + label, module.id().equals(selectedModuleId))) {
                selectedModuleId = module.id();
            }
        }
    }

    // ------------------------------------------------------------------
    // 右栏：选中模块的开关 + 参数
    // ------------------------------------------------------------------

    @Override
    public void renderDetails() {
        var host = entrance.getRenderManager().host();
        var registry = host.parameters();

        ImGuiShell.panelTitle("mcoo_build_with_ease.shell.details");

        var selected = host.modules().stream()
                .filter(module -> module.id().equals(selectedModuleId))
                .findFirst()
                .orElse(null);
        if (selected == null) {
            ImGuiWidgets.diagnostic("(select a module on the left)");
            return;
        }

        if (ImGuiShell.fieldCheckbox("mcoo_build_with_ease.render.enable_module",
                host.isModuleEnabled(selected.id()))) {
            host.setModuleEnabled(selected.id(), !host.isModuleEnabled(selected.id()));
            save();
        }

        ImGuiWidgets.separator();

        var parameters = selected.parameters();
        if (parameters.isEmpty()) {
            ImGuiWidgets.diagnostic("(this module has no parameters)");
            return;
        }
        for (var parameter : parameters) {
            if (ImGuiParameterBinder.draw(registry, selected.id(), parameter)) {
                save();
            }
        }
    }

    /** 把当前的模块开关与参数值写回配置文件。 */
    private void save() {
        var renderManager = entrance.getRenderManager();
        entrance.getConfigStorage().update(config ->
                config.withRenderConfig(renderManager.saveTo(config.renderConfig())));
    }

}
