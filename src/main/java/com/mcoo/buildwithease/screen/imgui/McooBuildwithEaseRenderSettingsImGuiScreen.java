package com.mcoo.buildwithease.screen.imgui;

import imgui.ImGui;
import imgui.type.ImInt;

import com.mcoo.buildwithease.McooBuildwithEaseClient;
import com.mcoo.buildwithease.screen.common.McooBuildwithEaseScreen;
import dev.huskuraft.universal.api.gui.ImGuiScreen;
import dev.huskuraft.universal.api.render.RenderModule;
import dev.huskuraft.universal.api.render.RenderParameter;
import dev.huskuraft.universal.api.render.RenderParameterRegistry;
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

        ImGui.setNextWindowSize(560f, 480f);
        if (ImGui.begin(Text.translate("mcoo_build_with_ease.render_settings.title").getString())) {
            // 总开关
            var master = host.isMasterEnabled();
            if (ImGui.checkbox(Text.translate("mcoo_build_with_ease.render.master_enable").getString(), master)) {
                host.setMasterEnabled(!master);
                save();
            }

            // 失败提示：哪个模块挂了
            var failures = host.failures();
            if (!failures.isEmpty()) {
                ImGui.separator();
                for (var failure : failures.entrySet()) {
                    ImGui.textWrapped(Text.translate("mcoo_build_with_ease.render.module_failed")
                            .getString() + ": " + failure.getKey());
                }
            }

            ImGui.separator();

            for (var module : host.modules()) {
                if (!ImGui.collapsingHeader(Text.translate(module.nameKey()).getString())) {
                    continue;
                }

                var enabled = host.isModuleEnabled(module.id());
                if (ImGui.checkbox(Text.translate("mcoo_build_with_ease.render.enable_module").getString(), enabled)) {
                    host.setModuleEnabled(module.id(), !enabled);
                    save();
                }

                for (var parameter : module.parameters()) {
                    drawParameter(registry, module, parameter);
                }
            }
        }
        ImGui.end();
    }

    private void drawParameter(RenderParameterRegistry registry, RenderModule module, RenderParameter parameter) {
        var label = Text.translate(parameter.labelKey()).getString();
        var value = registry.get(module.id(), parameter);

        switch (parameter.type()) {
            case BOOLEAN -> {
                var current = Boolean.TRUE.equals(value);
                if (ImGui.checkbox(label, current)) {
                    registry.set(module.id(), parameter, !current);
                    save();
                }
            }
            case INT -> {
                var holder = new int[]{value instanceof Number number ? number.intValue() : 0};
                if (ImGui.sliderInt(label, holder, (int) parameter.min(), (int) parameter.max())) {
                    registry.set(module.id(), parameter, holder[0]);
                    save();
                }
            }
            case FLOAT -> {
                var holder = new float[]{value instanceof Number number ? number.floatValue() : 0f};
                if (ImGui.sliderFloat(label, holder, (float) parameter.min(), (float) parameter.max())) {
                    registry.set(module.id(), parameter, holder[0]);
                    save();
                }
            }
            case CHOICE -> {
                var choices = parameter.choices();
                if (choices.isEmpty()) {
                    return;
                }
                var index = choices.indexOf(String.valueOf(value));
                var holder = new ImInt(index < 0 ? 0 : index);
                if (ImGui.combo(label, holder, choices.toArray(new String[0]))) {
                    registry.set(module.id(), parameter, choices.get(Math.min(holder.get(), choices.size() - 1)));
                    save();
                }
            }
            case COLOR -> {
                var argb = value instanceof Number number ? number.intValue() : 0xFFFFFFFF;
                var holder = new float[]{
                        ((argb >> 16) & 0xFF) / 255f,
                        ((argb >> 8) & 0xFF) / 255f,
                        (argb & 0xFF) / 255f,
                        ((argb >>> 24) & 0xFF) / 255f
                };
                if (ImGui.colorEdit4(label, holder)) {
                    registry.set(module.id(), parameter, toArgb(holder));
                    save();
                }
            }
            case STRING -> {
                // 文本参数暂时只读显示
                ImGui.labelText(label, String.valueOf(value));
            }
        }
    }

    private static int toArgb(float[] rgba) {
        var r = Math.round(rgba[0] * 255f) & 0xFF;
        var g = Math.round(rgba[1] * 255f) & 0xFF;
        var b = Math.round(rgba[2] * 255f) & 0xFF;
        var a = Math.round(rgba[3] * 255f) & 0xFF;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /** 把当前的模块开关与参数值写回配置文件。 */
    private void save() {
        var renderManager = getEntrance().getRenderManager();
        getEntrance().getConfigStorage().update(config ->
                config.withRenderConfig(renderManager.saveTo(config.renderConfig())));
    }

}
