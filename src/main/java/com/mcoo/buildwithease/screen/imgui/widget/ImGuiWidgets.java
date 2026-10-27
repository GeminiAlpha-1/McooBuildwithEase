package com.mcoo.buildwithease.screen.imgui.widget;

import dev.huskuraft.universal.api.text.Text;

/**
 * ImGui 控件封装。
 * <p>
 * <b>为什么要有这一层</b>：ImGui 是 immediate mode，控件调用直接写在绘制代码里，
 * 二十来个界面各写一遍就会出现「同一件事二十种写法」——改一个要改二十处。
 * 这一层把**所有界面共用的那部分**收拢：
 * <ul>
 *     <li>标签一律走 i18n（{@code Text.translate}），不直接写死字符串；</li>
 *     <li>控件的参数、范围、返回值统一；</li>
 *     <li>想换控件外观 / 加主题，只改这里。</li>
 * </ul>
 * 界面里只写「我要一个什么控件」，不写 ImGui 的具体调用。
 */
public final class ImGuiWidgets {

    private ImGuiWidgets() {
    }

    // ------------------------------------------------------------------
    // 文本
    // ------------------------------------------------------------------

    public static void text(String labelKey) {
        imgui.ImGui.text(translate(labelKey));
    }

    public static void textWrapped(String labelKey) {
        imgui.ImGui.textWrapped(translate(labelKey));
    }

    /** 带前缀的文本（{@code 标签: 值}）。 */
    public static void labelText(String labelKey, String value) {
        imgui.ImGui.labelText(translate(labelKey), value);
    }

    public static void separator() {
        imgui.ImGui.separator();
    }

    public static void spacing() {
        imgui.ImGui.spacing();
    }

    // ------------------------------------------------------------------
    // 容器
    // ------------------------------------------------------------------

    public static boolean collapsingHeader(String labelKey) {
        return imgui.ImGui.collapsingHeader(translate(labelKey));
    }

    /** 打开一个窗口；返回 false 表示窗口被折叠（内容可以不画）。 */
    public static boolean beginWindow(String titleKey, float width, float height) {
        imgui.ImGui.setNextWindowSize(width, height);
        return imgui.ImGui.begin(translate(titleKey));
    }

    public static void endWindow() {
        imgui.ImGui.end();
    }

    // ------------------------------------------------------------------
    // 控件
    // ------------------------------------------------------------------

    public static boolean checkbox(String labelKey, boolean value) {
        return imgui.ImGui.checkbox(translate(labelKey), value);
    }

    public static boolean button(String labelKey) {
        return imgui.ImGui.button(translate(labelKey));
    }

    public static float sliderFloat(String labelKey, float value, float min, float max) {
        var holder = new float[]{value};
        imgui.ImGui.sliderFloat(translate(labelKey), holder, min, max);
        return holder[0];
    }

    public static int sliderInt(String labelKey, int value, int min, int max) {
        var holder = new int[]{value};
        imgui.ImGui.sliderInt(translate(labelKey), holder, min, max);
        return holder[0];
    }

    /**
     * 下拉选择。
     *
     * @param items       选项（原文）
     * @param currentIndex 当前选中项
     * @return 新的选中项（没变就是原值）
     */
    public static int combo(String labelKey, String[] items, int currentIndex) {
        if (items.length == 0) {
            return currentIndex;
        }
        var holder = new imgui.type.ImInt(Math.max(0, Math.min(currentIndex, items.length - 1)));
        imgui.ImGui.combo(translate(labelKey), holder, items);
        return holder.get();
    }

    /**
     * 颜色选择（ARGB 整数 ↔ ImGui 的 RGBA 浮点四元组）。
     *
     * @return 新的 ARGB 值
     */
    public static int colorEdit(String labelKey, int argb) {
        var holder = new float[]{
                ((argb >> 16) & 0xFF) / 255f,
                ((argb >> 8) & 0xFF) / 255f,
                (argb & 0xFF) / 255f,
                ((argb >>> 24) & 0xFF) / 255f
        };
        imgui.ImGui.colorEdit4(translate(labelKey), holder);
        var r = Math.round(holder[0] * 255f) & 0xFF;
        var g = Math.round(holder[1] * 255f) & 0xFF;
        var b = Math.round(holder[2] * 255f) & 0xFF;
        var a = Math.round(holder[3] * 255f) & 0xFF;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /** 文本输入框。 */
    public static String inputText(String labelKey, String value) {
        var holder = new imgui.type.ImString(value == null ? "" : value);
        imgui.ImGui.inputText(translate(labelKey), holder, 256);
        return holder.get();
    }

    /** 可选中的条目（列表里的一项）。 */
    public static boolean selectable(String labelKey, boolean selected) {
        return imgui.ImGui.selectable(translate(labelKey), selected);
    }

    /** 滚动子区域（列表容器）。 */
    public static boolean beginChild(String labelKey, float width, float height) {
        return imgui.ImGui.beginChild(translate(labelKey), width, height, true);
    }

    public static void endChild() {
        imgui.ImGui.endChild();
    }

    private static String translate(String labelKey) {
        return Text.translate(labelKey).getString();
    }

}
