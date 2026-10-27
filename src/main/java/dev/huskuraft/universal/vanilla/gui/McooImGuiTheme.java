package dev.huskuraft.universal.vanilla.gui;

import imgui.ImGui;
import imgui.ImGuiStyle;
import imgui.flag.ImGuiCol;

/**
 * 本模组的 ImGui 主题：<b>深色、方角、细边</b>，对齐 Axiom 那套"编辑器"观感。
 *
 * <h3>为什么集中在这里</h3>
 * ImGui 的样式是<b>全局上下文状态</b>，谁都能改。散在各个界面里改颜色，
 * 最后就是"每个界面一套色"，改一处漏三处。所以样式只在本类定义一次，
 * 界面里<b>不要</b>直接写 {@code pushStyleColor} / {@code setColor}。
 *
 * <h3>配色来源</h3>
 * 取 Axiom 编辑器界面的观感（近黑底 + 蓝色高亮 + 1px 分隔线），数值是按截图目测调的，
 * 不是抄它的代码（Axiom 是 ARR）。要微调只改下面的常量表。
 */
final class McooImGuiTheme {

    // ---- 底色 ----
    private static final float[] BG_WINDOW = {0.086f, 0.086f, 0.094f, 0.96f};
    private static final float[] BG_CHILD = {0.102f, 0.102f, 0.114f, 1.00f};
    private static final float[] BG_POPUP = {0.086f, 0.086f, 0.094f, 1.00f};
    private static final float[] BG_HEADER = {0.157f, 0.157f, 0.180f, 1.00f};
    private static final float[] BG_HEADER_HOVER = {0.208f, 0.208f, 0.243f, 1.00f};
    private static final float[] BG_HEADER_ACTIVE = {0.259f, 0.259f, 0.302f, 1.00f};

    // ---- 控件 ----
    private static final float[] FRAME_BG = {0.145f, 0.145f, 0.165f, 1.00f};
    private static final float[] FRAME_BG_HOVER = {0.196f, 0.196f, 0.227f, 1.00f};
    private static final float[] FRAME_BG_ACTIVE = {0.239f, 0.239f, 0.278f, 1.00f};
    private static final float[] BUTTON = {0.169f, 0.169f, 0.192f, 1.00f};
    private static final float[] BUTTON_HOVER = {0.227f, 0.227f, 0.259f, 1.00f};
    private static final float[] BUTTON_ACTIVE = {0.290f, 0.290f, 0.333f, 1.00f};

    // ---- 线 ----
    private static final float[] BORDER = {0.196f, 0.196f, 0.220f, 1.00f};
    private static final float[] SEPARATOR = {0.239f, 0.239f, 0.275f, 1.00f};

    // ---- 文字 ----
    private static final float[] TEXT = {0.878f, 0.878f, 0.906f, 1.00f};
    private static final float[] TEXT_DISABLED = {0.451f, 0.451f, 0.486f, 1.00f};
    private static final float[] TEXT_SELECTED_BG = {0.180f, 0.337f, 0.561f, 0.85f};

    // ---- 强调（进度条 / 选中 / 滑块的抓取点）----
    private static final float[] ACCENT = {0.259f, 0.518f, 0.808f, 1.00f};
    private static final float[] ACCENT_HOVER = {0.310f, 0.580f, 0.870f, 1.00f};
    private static final float[] ACCENT_ACTIVE = {0.220f, 0.450f, 0.740f, 1.00f};

    // ---- 滚动条 ----
    private static final float[] SCROLL_BG = {0.063f, 0.063f, 0.071f, 1.00f};
    private static final float[] SCROLL_GRAB = {0.239f, 0.239f, 0.275f, 1.00f};
    private static final float[] SCROLL_GRAB_HOVER = {0.318f, 0.318f, 0.361f, 1.00f};
    private static final float[] SCROLL_GRAB_ACTIVE = {0.380f, 0.380f, 0.431f, 1.00f};

    private McooImGuiTheme() {
    }

    /** 套用主题。由 {@link McooImGuiService#configure} 调一次（此时 ImGui 上下文已存在）。 */
    static void apply(float uiScale) {
        ImGuiStyle style = ImGui.getStyle();

        style.setWindowRounding(0f);
        style.setChildRounding(0f);
        style.setPopupRounding(0f);
        style.setFrameRounding(2f);
        style.setGrabRounding(2f);
        style.setScrollbarRounding(2f);
        style.setTabRounding(2f);

        style.setWindowBorderSize(1f);
        style.setChildBorderSize(1f);
        style.setPopupBorderSize(1f);
        style.setFrameBorderSize(0f);

        style.setWindowPadding(8f, 6f);
        style.setFramePadding(6f, 3f);
        style.setItemSpacing(6f, 4f);
        style.setItemInnerSpacing(4f, 3f);
        style.setScrollbarSize(10f);
        style.setIndentSpacing(14f);
        style.setWindowTitleAlign(0f, 0.5f);
        style.setButtonTextAlign(0.5f, 0.5f);
        style.setSelectableTextAlign(0f, 0.5f);

        color(style, ImGuiCol.Text, TEXT);
        color(style, ImGuiCol.TextDisabled, TEXT_DISABLED);
        color(style, ImGuiCol.WindowBg, BG_WINDOW);
        color(style, ImGuiCol.ChildBg, BG_CHILD);
        color(style, ImGuiCol.PopupBg, BG_POPUP);
        color(style, ImGuiCol.Border, BORDER);
        color(style, ImGuiCol.BorderShadow, 0f, 0f, 0f, 0f);
        color(style, ImGuiCol.FrameBg, FRAME_BG);
        color(style, ImGuiCol.FrameBgHovered, FRAME_BG_HOVER);
        color(style, ImGuiCol.FrameBgActive, FRAME_BG_ACTIVE);
        color(style, ImGuiCol.TitleBg, BG_WINDOW);
        color(style, ImGuiCol.TitleBgActive, BG_CHILD);
        color(style, ImGuiCol.TitleBgCollapsed, BG_WINDOW);
        color(style, ImGuiCol.MenuBarBg, BG_CHILD);
        color(style, ImGuiCol.ScrollbarBg, SCROLL_BG);
        color(style, ImGuiCol.ScrollbarGrab, SCROLL_GRAB);
        color(style, ImGuiCol.ScrollbarGrabHovered, SCROLL_GRAB_HOVER);
        color(style, ImGuiCol.ScrollbarGrabActive, SCROLL_GRAB_ACTIVE);
        color(style, ImGuiCol.CheckMark, ACCENT);
        color(style, ImGuiCol.SliderGrab, ACCENT);
        color(style, ImGuiCol.SliderGrabActive, ACCENT_HOVER);
        color(style, ImGuiCol.Button, BUTTON);
        color(style, ImGuiCol.ButtonHovered, BUTTON_HOVER);
        color(style, ImGuiCol.ButtonActive, BUTTON_ACTIVE);
        color(style, ImGuiCol.Header, BG_HEADER);
        color(style, ImGuiCol.HeaderHovered, BG_HEADER_HOVER);
        color(style, ImGuiCol.HeaderActive, BG_HEADER_ACTIVE);
        color(style, ImGuiCol.Separator, SEPARATOR);
        color(style, ImGuiCol.SeparatorHovered, ACCENT_HOVER);
        color(style, ImGuiCol.SeparatorActive, ACCENT_ACTIVE);
        color(style, ImGuiCol.ResizeGrip, SEPARATOR);
        color(style, ImGuiCol.ResizeGripHovered, ACCENT_HOVER);
        color(style, ImGuiCol.ResizeGripActive, ACCENT_ACTIVE);
        color(style, ImGuiCol.TextSelectedBg, TEXT_SELECTED_BG);
        color(style, ImGuiCol.Tab, BG_CHILD);
        color(style, ImGuiCol.TabHovered, BG_HEADER_HOVER);
        color(style, ImGuiCol.TabSelected, BG_HEADER);
        color(style, ImGuiCol.TableHeaderBg, BG_CHILD);
        color(style, ImGuiCol.TableBorderStrong, BORDER);
        color(style, ImGuiCol.TableBorderLight, SEPARATOR);

        // 所有尺寸按 UI 缩放一起放大，字号以外的间距/控件高度才不会在大分辨率下显得拥挤
        if (uiScale != 1.0f) {
            style.scaleAllSizes(uiScale);
        }
    }

    private static void color(ImGuiStyle style, int index, float[] rgba) {
        color(style, index, rgba[0], rgba[1], rgba[2], rgba[3]);
    }

    private static void color(ImGuiStyle style, int index, float r, float g, float b, float a) {
        style.setColor(index, r, g, b, a);
    }

}
