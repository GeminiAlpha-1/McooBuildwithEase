package com.mcoo.buildwithease.screen.imgui.widget;

import dev.huskuraft.universal.api.text.Text;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiChildFlags;
import imgui.flag.ImGuiSelectableFlags;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;

/**
 * ImGui 界面的<b>壳</b>：全屏铺满 + 三栏（左侧工具列表 / 中间内容 / 右侧详情）。
 *
 * <h3>为什么不是「居中带标题栏的窗口」</h3>
 * 默认那种 ImGui 窗口观感和本模组要的编辑器风格不符。目标是 Axiom / Photoshop 那类排布：
 * 面板贴边、横向分栏、右侧放当前对象的详情（用户 2026-10-27 明确要求）。
 *
 * <h3>用法</h3>
 * <pre>{@code
 * if (!ImGuiShell.begin()) { ImGuiShell.end(); return; }
 * try {
 *     float left = ImGuiShell.suggestedLeftWidth();
 *     float right = ImGuiShell.suggestedRightWidth();
 *     ImGuiShell.layout(left, right, this::drawTools, this::drawContent, this::drawDetails);
 * } finally {
 *     ImGuiShell.end();
 * }
 * }</pre>
 *
 * 注意 {@code ImGui.begin()} 返回 false 时<b>仍要</b>调 {@code end()} —— 这是 ImGui 的硬性约定。
 */
public final class ImGuiShell {

    /** 根窗口的 id：以 {@code ##} 开头 = 名字里只有 id 部分参与哈希，标题不显示。 */
    private static final String ROOT_ID = "##mcoo_shell_root";

    private static final String LEFT_ID = "##mcoo_shell_left";
    private static final String CENTER_ID = "##mcoo_shell_center";
    private static final String RIGHT_ID = "##mcoo_shell_right";

    /** 面板之间的缝（px）。 */
    private static final float GAP = 4f;

    /** 左侧栏宽度下限（窄窗口下不至于挤成一列字）。 */
    private static final float MIN_LEFT_WIDTH = 140f;
    private static final float MIN_RIGHT_WIDTH = 160f;

    private static final float LEFT_RATIO = 0.16f;
    private static final float RIGHT_RATIO = 0.24f;

    /** 右栏「标签在左、控件在右」时的标签列宽。 */
    private static final float LABEL_COLUMN = 76f;

    private ImGuiShell() {
    }

    /** 铺满整个屏幕的根窗口。返回值同 {@code ImGui.begin()}。 */
    public static boolean begin() {
        ImVec2 display = ImGui.getIO().getDisplaySize();
        ImGui.setNextWindowPos(0f, 0f);
        ImGui.setNextWindowSize(display.x, display.y);
        int flags = ImGuiWindowFlags.NoTitleBar
                | ImGuiWindowFlags.NoResize
                | ImGuiWindowFlags.NoMove
                | ImGuiWindowFlags.NoScrollbar
                | ImGuiWindowFlags.NoScrollWithMouse
                | ImGuiWindowFlags.NoCollapse
                | ImGuiWindowFlags.NoSavedSettings
                | ImGuiWindowFlags.NoBringToFrontOnFocus
                // 根窗口透明：中间的内容区要能看见世界（3D 预览 / 建造过程），
                // 面板自己的底色来自 ChildBg。Axiom 也是这个结构。
                | ImGuiWindowFlags.NoBackground;
        // 根窗口要零内边距，面板才能贴到屏幕边缘；window padding 只在 begin 时读一次，
        // 所以 begin 之后立刻 pop 掉即可。
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 0f, 0f);
        boolean open = ImGui.begin(ROOT_ID, flags);
        ImGui.popStyleVar();
        return open;
    }

    public static void end() {
        ImGui.end();
    }

    public static float suggestedLeftWidth() {
        float width = ImGui.getIO().getDisplaySizeX();
        return Math.max(MIN_LEFT_WIDTH, width * LEFT_RATIO);
    }

    public static float suggestedRightWidth() {
        float width = ImGui.getIO().getDisplaySizeX();
        return Math.max(MIN_RIGHT_WIDTH, width * RIGHT_RATIO);
    }

    /**
     * 三栏布局。三个 body 依次在左 / 中 / 右三栏里执行。
     * <p>
     * 中间栏吃掉剩余宽度（减掉两条缝），所以窗口缩放时只有它会变宽变窄。
     */
    public static void layout(float leftWidth, float rightWidth,
                              Runnable left, Runnable center, Runnable right) {
        ImVec2 avail = ImGui.getContentRegionAvail();
        float height = avail.y;
        float centerWidth = Math.max(1f, avail.x - leftWidth - rightWidth - GAP * 2f);

        panel(LEFT_ID, leftWidth, height, left);
        ImGui.sameLine(0f, GAP);
        panel(CENTER_ID, centerWidth, height, center);
        ImGui.sameLine(0f, GAP);
        panel(RIGHT_ID, rightWidth, height, right);
    }

    private static void panel(String id, float width, float height, Runnable body) {
        // Borders：一像素描边 —— 就是 Axiom 那种「面板之间有条缝」的观感
        ImGui.beginChild(id, width, height, ImGuiChildFlags.Borders);
        try {
            // 中间栏要画原生 3D 预览（平台的画中画渲染需要**绝对屏幕坐标**），
            // 所以在进入 body 之前把这一栏的屏幕矩形记下来。
            if (CENTER_ID.equals(id)) {
                var origin = ImGui.getCursorScreenPos();
                centerX = Math.round(origin.x);
                centerY = Math.round(origin.y);
                centerWidth = Math.round(width);
                centerHeight = Math.round(height);
            }
            body.run();
        } finally {
            ImGui.endChild();
        }
    }

    // 中间栏最近一次的屏幕矩形（GUI 逻辑像素）。只给「需要原生渲染的面板」用。
    private static int centerX;
    private static int centerY;
    private static int centerWidth;
    private static int centerHeight;

    /**
     * 中间栏最近一帧的屏幕矩形，{@code {x, y, width, height}}（GUI 逻辑像素）。
     * <p>
     * 给需要 MC 原生渲染的面板用（例如剪贴板的 3D 预览）——
     * 平台层的画中画渲染按绝对坐标工作，ImGui 那边只有相对光标位置。
     * <p>
     * ⚠️ 这是<b>上一帧</b>布局的结果（本帧还没画完），窗口尺寸不变时完全够用。
     */
    public static int[] centerRect() {
        return new int[]{centerX, centerY, centerWidth, centerHeight};
    }

    // ------------------------------------------------------------------
    // 左栏：分组 + 导航条目
    // ------------------------------------------------------------------

    /** 分组标题（左栏用）。 */
    public static void section(String labelKey) {
        ImGui.spacing();
        ImGui.separatorText(translate(labelKey));
    }

    /**
     * 一条导航项（整行可点）。
     *
     * @return 本帧是否被点击
     */
    public static boolean navItem(String labelKey, boolean selected) {
        return navItemRaw(translate(labelKey), selected);
    }

    /**
     * 同 {@link #navItem}，但标签已经是<b>成品文本</b>（动态拼接的行，不是 i18n 键）。
     * <p>
     * 用于列表项：模块名、方块名、文件名这类内容本身就是数据、翻译在数据源里做过了。
     */
    public static boolean navItemRaw(String label, boolean selected) {
        float width = ImGui.getContentRegionAvail().x;
        float height = ImGui.getTextLineHeight() + 6f;
        return ImGui.selectable(label, selected,
                ImGuiSelectableFlags.None, width, height);
    }

    /** 左栏顶部的标题（不做成可点条目）。 */
    public static void panelTitle(String labelKey) {
        ImGui.textDisabled(translate(labelKey));
        ImGui.separator();
    }

    /** 右栏的分组（可折叠）。 */
    public static boolean collapsible(String labelKey, boolean defaultOpen) {
        return ImGui.collapsingHeader(translate(labelKey),
                defaultOpen ? imgui.flag.ImGuiTreeNodeFlags.DefaultOpen : imgui.flag.ImGuiTreeNodeFlags.None);
    }

    /**
     * 内容区占位块：填满可用空间、带描边、正中一行居中提示。
     * <p>
     * 真实内容（3D 预览 / 列表 / 表格）接进来之前，先用它占住位置，
     * 这样分栏比例能否对齐一眼就能看出来。
     */
    public static void contentPlaceholder(String labelKey) {
        ImVec2 avail = ImGui.getContentRegionAvail();
        ImGui.beginChild("##mcoo_shell_placeholder", avail.x, avail.y, ImGuiChildFlags.Borders);
        try {
            String hint = translate(labelKey);
            float textWidth = ImGui.calcTextSize(hint).x;
            ImVec2 inner = ImGui.getContentRegionAvail();
            ImGui.setCursorPos((inner.x - textWidth) / 2f, inner.y / 2f);
            ImGui.textDisabled(hint);
        } finally {
            ImGui.endChild();
        }
    }

    // ------------------------------------------------------------------
    // 右栏：标签在左、控件在右（ImGui 默认把标签放右边，那是工具条的风格，
    //       属性面板要的是「名字 | 值」两列对齐）
    // ------------------------------------------------------------------

    /** 整数滑条（左标签 + 右滑条，滑条吃满剩余宽度）。 */
    public static int fieldSliderInt(String labelKey, int value, int min, int max) {
        labelColumn(labelKey);
        ImGui.setNextItemWidth(-1f);
        int[] holder = {value};
        ImGui.sliderInt("##field_" + labelKey, holder, min, max);
        return holder[0];
    }

    /** 浮点滑条（左标签 + 右滑条）。 */
    public static float fieldSliderFloat(String labelKey, float value, float min, float max) {
        labelColumn(labelKey);
        ImGui.setNextItemWidth(-1f);
        float[] holder = {value};
        ImGui.sliderFloat("##field_" + labelKey, holder, min, max);
        return holder[0];
    }

    /** 开关（左标签 + 右勾选框）。返回<b>新值</b>（ImGui 的 checkbox 语义：点击即翻转）。 */
    public static boolean fieldCheckbox(String labelKey, boolean value) {
        ImGui.alignTextToFramePadding();
        ImGui.text(translate(labelKey));
        ImGui.sameLine(LABEL_COLUMN);
        return ImGui.checkbox("##field_" + labelKey, value);
    }

    /** 只读的一行「标签 : 值」，用于展示不可编辑的信息。 */
    public static void fieldText(String labelKey, String value) {
        labelColumn(labelKey);
        ImGui.textDisabled(value);
    }

    /** 下拉选择（左标签 + 右下拉）。 */
    public static int fieldCombo(String labelKey, String[] items, int currentIndex) {
        if (items.length == 0) {
            return currentIndex;
        }
        labelColumn(labelKey);
        ImGui.setNextItemWidth(-1f);
        var holder = new imgui.type.ImInt(Math.max(0, Math.min(currentIndex, items.length - 1)));
        ImGui.combo("##field_" + labelKey, holder, items);
        return holder.get();
    }

    /** 颜色选择（左标签 + 右色块）。 */
    public static int fieldColor(String labelKey, int argb) {
        labelColumn(labelKey);
        ImGui.setNextItemWidth(-1f);
        float[] rgba = {
                ((argb >> 16) & 0xFF) / 255f,
                ((argb >> 8) & 0xFF) / 255f,
                (argb & 0xFF) / 255f,
                ((argb >>> 24) & 0xFF) / 255f,
        };
        ImGui.colorEdit4("##field_" + labelKey, rgba);
        int r = Math.round(rgba[0] * 255f) & 0xFF;
        int g = Math.round(rgba[1] * 255f) & 0xFF;
        int b = Math.round(rgba[2] * 255f) & 0xFF;
        int a = Math.round(rgba[3] * 255f) & 0xFF;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static void labelColumn(String labelKey) {
        ImGui.alignTextToFramePadding();
        ImGui.text(translate(labelKey));
        ImGui.sameLine(LABEL_COLUMN);
    }

    /** 把光标推到面板底部，用来把「完成 / 取消」按到底边。 */
    public static void bottomAlign(float reservedHeight) {
        float remaining = ImGui.getContentRegionAvail().y - reservedHeight;
        if (remaining > 0f) {
            ImGui.dummy(0f, remaining);
        }
    }

    /** 同上，预留「一行控件 + 一行间距」的高度（调用方不用碰 ImGui 常量）。 */
    public static void bottomAlign() {
        bottomAlign(ImGui.getFrameHeight() + ImGui.getStyle().getItemSpacingY());
    }

    private static String translate(String labelKey) {
        return Text.translate(labelKey).getString();
    }

}
