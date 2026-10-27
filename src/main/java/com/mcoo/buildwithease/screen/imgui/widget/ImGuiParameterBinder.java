package com.mcoo.buildwithease.screen.imgui.widget;

import dev.huskuraft.universal.api.render.RenderParameter;
import dev.huskuraft.universal.api.render.RenderParameterRegistry;

/**
 * 把 {@link RenderParameter}（参数声明）画成对应的 ImGui 控件。
 *
 * <h3>为什么单独一层</h3>
 * 参数是<b>模块自己声明</b>的，界面不该知道每个参数是滑条还是下拉 ——
 * 这一层负责「声明 → 控件」的映射，加一种参数类型只改这里，界面一行不动。
 *
 * <h3>为什么都走 {@link ImGuiShell} 的 field* 系列</h3>
 * 属性面板是「名字 | 值」两列对齐的形态；ImGui 默认把标签放在控件右边（那是工具条的形态）。
 * 两列对齐能让一列参数读起来是整齐的表格 —— 这里统一用左标签版本。
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
                // fieldCheckbox 返回的是「新值」；与当前不同才算改了
                var updated = ImGuiShell.fieldCheckbox(parameter.labelKey(), current);
                if (updated != current) {
                    registry.set(moduleId, parameter, updated);
                    yield true;
                }
                yield false;
            }
            case INT -> {
                var current = value instanceof Number number ? number.intValue() : 0;
                var updated = ImGuiShell.fieldSliderInt(parameter.labelKey(), current,
                        (int) parameter.min(), (int) parameter.max());
                if (updated != current) {
                    registry.set(moduleId, parameter, updated);
                    yield true;
                }
                yield false;
            }
            case FLOAT -> {
                var current = value instanceof Number number ? number.floatValue() : 0f;
                var updated = ImGuiShell.fieldSliderFloat(parameter.labelKey(), current,
                        (float) parameter.min(), (float) parameter.max());
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
                var index = ImGuiShell.fieldCombo(parameter.labelKey(),
                        choices.toArray(new String[0]), current);
                if (index != current) {
                    registry.set(moduleId, parameter, choices.get(Math.min(index, choices.size() - 1)));
                    yield true;
                }
                yield false;
            }
            case COLOR -> {
                var current = value instanceof Number number ? number.intValue() : 0xFFFFFFFF;
                var updated = ImGuiShell.fieldColor(parameter.labelKey(), current);
                if (updated != current) {
                    registry.set(moduleId, parameter, updated);
                    yield true;
                }
                yield false;
            }
            case STRING -> {
                // 文本参数暂时只读显示
                ImGuiShell.fieldText(parameter.labelKey(), String.valueOf(value));
                yield false;
            }
        };
    }

}
