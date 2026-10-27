package com.mcoo.buildwithease.screen.imgui;

import com.mcoo.buildwithease.McooBuildwithEaseClient;
import com.mcoo.buildwithease.screen.common.McooBuildwithEaseScreen;
import com.mcoo.buildwithease.screen.imgui.widget.ImGuiParameterBinder;
import com.mcoo.buildwithease.screen.imgui.widget.ImGuiWidgets;
import dev.huskuraft.universal.api.gui.ImGuiScreen;
import dev.huskuraft.universal.api.text.Text;

/**
 * 渲染设置（ImGui 版）。
 * <p>
 * 本 mod 自研的 GUI 功能太少，界面一律改用 Dear ImGui 画。
 * 这个界面同时也是**渲染模块框架的展示窗口** ——
 * 13 个模块、58 个参数全部自动列出来（参数由模块自己声明，这里只负责画）。
 * <p>
 * 以后再加模块 / 加参数，这里一行都不用改。
 */
public class McooBuildwithEaseRenderSettingsImGuiScreen extends McooBuildwithEaseScreen implements ImGuiScreen {

    public McooBuildwithEaseRenderSettingsImGuiScreen(McooBuildwithEaseClient entrance) {
        super(entrance, Text.translate("mcoo_build_with_ease.render_settings.title"));
    }

    @Override
    public void renderImGui() {
        var host = getEntrance().getRenderManager().host();
        var registry = host.parameters();

        if (ImGuiWidgets.beginWindow("mcoo_build_with_ease.render_settings.title", 560f, 480f)) {
            // 总开关
            var master = host.isMasterEnabled();
            if (ImGuiWidgets.checkbox("mcoo_build_with_ease.render.master_enable", master)) {
                host.setMasterEnabled(!master);
                save();
            }

            // 失败提示：哪个模块挂了
            var failures = host.failures();
            if (!failures.isEmpty()) {
                ImGuiWidgets.separator();
                for (var failure : failures.keySet()) {
                    ImGuiWidgets.textWrapped("mcoo_build_with_ease.render.module_failed");
                    ImGuiWidgets.text("  " + failure);
                }
            }

            ImGuiWidgets.separator();

            for (var module : host.modules()) {
                if (!ImGuiWidgets.collapsingHeader(module.nameKey())) {
                    continue;
                }

                var enabled = host.isModuleEnabled(module.id());
                if (ImGuiWidgets.checkbox("mcoo_build_with_ease.render.enable_module", enabled)) {
                    host.setModuleEnabled(module.id(), !enabled);
                    save();
                }

                for (var parameter : module.parameters()) {
                    if (ImGuiParameterBinder.draw(registry, module.id(), parameter)) {
                        save();
                    }
                }
            }
        }
        ImGuiWidgets.endWindow();
    }

    /** 把当前的模块开关与参数值写回配置文件。 */
    private void save() {
        var renderManager = getEntrance().getRenderManager();
        getEntrance().getConfigStorage().update(config ->
                config.withRenderConfig(renderManager.saveTo(config.renderConfig())));
    }

}
