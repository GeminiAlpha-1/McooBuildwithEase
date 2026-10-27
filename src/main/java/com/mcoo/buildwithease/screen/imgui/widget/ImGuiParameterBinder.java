package com.mcoo.buildwithease.screen.imgui.widget;

import dev.huskuraft.universal.api.render.RenderParameter;
import dev.huskuraft.universal.api.render.RenderParameterRegistry;

/**
 * 把 {@link RenderParameter}（参数声明）画成对应的 ImGui 控件。
 * <p>
 * <b>为什么单独一层</b>：参数是**模块自己声明**的，界面不该知道每个参数是滑条还是下拉 ——
 * 这一层负责「声明 → 控件」的映射，加一种参数类型只改这里，界面一行不动。
 */
public final class ImGuiParameterBinder {

    private ImGuiParameterBinder() {
    }

    /**
     * 画一个参数。
     *
     * @return 值是否被改了（改了的话调用方负责写回配置）
     */
    public static boolean draw(RenderParameterRegistry registry, String moduleId, RenderParameter parameter) {
        var value = registry.get(moduleId, parameter);
        return switch (parameter.type()) {
            case BOOLEAN -> {
                var current = Boolean.TRUE.equals(value);
                var changed = ImGuiWidgets.checkbox(parameter.labelKey(), current);
                if (changed) {
                    registry.set(moduleId, parameter, !current);
                }
                yield changed;
            }
            case INT -> {
                var current = value instanceof Number number ? number.intValue() : 0;
                var updated = ImGuiWidgets.sliderInt(parameter.labelKey(), current, (int) parameter.min(), (int) parameter.max());
                if (updated != current) {
                    registry.set(moduleId, parameter, updated);
                    yield true;
                }
                yield false;
            }
            case FLOAT -> {
                var current = value instanceof Number number ? number.floatValue() : 0f;
                var updated = ImGuiWidgets.sliderFloat(parameter.labelKey(), current, (float) parameter.min(), (float) parameter.max());
                if (updated != current) {
                    registry.set(moduleId, parameter, updated);
                    yield true;
                }
                yield false;
            }
            case CHOICE -> {
                var choices = parameter.choices();
                if (choices.isEmpty()) {
                    yield false;
                }
                var current = choices.indexOf(String.valueOf(value));
                var index = ImGuiWidgets.combo(parameter.labelKey(), choices.toArray(new String[0]), current);
                if (index != current) {
                    registry.set(moduleId, parameter, choices.get(Math.min(index, choices.size() - 1)));
                    yield true;
                }
                yield false;
            }
            case COLOR -> {
                var current = value instanceof Number number ? number.intValue() : 0xFFFFFFFF;
                var updated = ImGuiWidgets.colorEdit(parameter.labelKey(), current);
                if (updated != current) {
                    registry.set(moduleId, parameter, updated);
                    yield true;
                }
                yield false;
            }
            case STRING -> {
                // 文本参数暂时只读显示
                ImGuiWidgets.labelText(parameter.labelKey(), String.valueOf(value));
                yield false;
            }
        };
    }

}
