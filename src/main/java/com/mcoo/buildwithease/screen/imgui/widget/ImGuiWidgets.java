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

    // ------------------------------------------------------------------
    // 图像
    // ------------------------------------------------------------------

    /**
     * 画一块纹理的指定 UV 区域（图标就靠它）。
     *
     * @param textureId ImGui 纹理 id，来源见 {@code McooImGuiTextures}（**每帧现取**，不要跨帧缓存）
     * @param u0v0u1v1  纹理坐标：{@code {u0, v0, u1, v1}}；传 {@code null} 表示整张纹理
     */
    public static void image(long textureId, float width, float height, float[] u0v0u1v1) {
        if (textureId == 0L) {
            // 拿不到纹理时画个占位，别让界面出现"什么都没有"的错觉
            imgui.ImGui.dummy(width, height);
            return;
        }
        if (u0v0u1v1 == null || u0v0u1v1.length < 4) {
            imgui.ImGui.image(textureId, width, height);
            return;
        }
        imgui.ImGui.image(textureId, width, height,
                u0v0u1v1[0], u0v0u1v1[1], u0v0u1v1[2], u0v0u1v1[3]);
    }

    /**
     * 诊断用的原文输出（<b>不走 i18n</b>）。
     * <p>
     * 只给调试/原型界面打印数值用（纹理 id、UV、图集尺寸之类）。
     * 面向玩家的文案一律走 {@link #text(String)} 那个 i18n 版本。
     */
    public static void diagnostic(String text) {
        imgui.ImGui.textDisabled(text);
    }

    // ------------------------------------------------------------------
    // 方块图标（等轴测）
    // ------------------------------------------------------------------

    /** 顶面 / 左面 / 右面的明暗（ImU32 = 0xAABBGGRR，灰度三面一致即可）。 */
    private static final int FACE_TOP = 0xFFFFFFFF;
    private static final int FACE_LEFT = 0xFFB4B4B4;
    private static final int FACE_RIGHT = 0xFF8C8C8C;

    /**
     * 画一个<b>等轴测方块图标</b>：同一张方块贴图凑出「顶面 + 左面 + 右面」，三面各自调亮度。
     *
     * <h3>为什么不用离屏 3D 烘图</h3>
     * 真 3D 要建 FBO、重定向 render pass（{@code RenderSystem.outputColorTextureOverride}）、
     * 还要处理"不能在 ImGui 帧里烘"的顺序问题 —— 风险大、跟版成本高。
     * 而方块图标要的只是"看着像个立方体"，用三个四边形拼就够了：
     * 零 GPU 资源、零 pass 嵌套、OpenGL 与 Vulkan 都一样。
     * <p>
     * ⚠️ 贴图会被拉伸到面上（方块贴图本来就是 16×16 平铺，观感正常）；
     * 非立方体方块（楼梯、栅栏）用这张图标会失真 —— 那种留给将来的真 3D 烘图。
     *
     * @param textureId 纹理 id（见 {@code McooImGuiTextures}，每帧现取）
     * @param uv        sprite 的 {@code {u0, v0, u1, v1}}
     * @param size      图标外接正方形边长
     */
    public static void blockIcon(long textureId, float[] uv, float size) {
        if (textureId == 0L || uv == null || uv.length < 4) {
            imgui.ImGui.dummy(size, size);
            return;
        }
        var drawList = imgui.ImGui.getWindowDrawList();
        var origin = imgui.ImGui.getCursorScreenPos();

        float half = size * 0.5f;          // 半宽
        float quarter = half * 0.5f;       // 菱形的半高（= 半宽/2，等轴测的 2:1 斜度）
        float depth = half;                // 侧面垂直高度
        float cx = origin.x + half;
        float topY = origin.y;

        // 顶面菱形的四个顶点（上 / 右 / 下 / 左）
        float tX = cx, tY = topY;
        float rX = cx + half, rY = topY + quarter;
        float bX = cx, bY = topY + quarter * 2f;
        float lX = cx - half, lY = topY + quarter;

        float u0 = uv[0], v0 = uv[1], u1 = uv[2], v1 = uv[3];

        // 顶面
        drawList.addImageQuad(textureId,
                tX, tY, rX, rY, bX, bY, lX, lY,
                u0, v0, u1, v0, u1, v1, u0, v1,
                FACE_TOP);

        // 左面：菱形左下边向下拉 depth
        drawList.addImageQuad(textureId,
                lX, lY, bX, bY, bX, bY + depth, lX, lY + depth,
                u0, v0, u1, v0, u1, v1, u0, v1,
                FACE_LEFT);

        // 右面：菱形右下边向下拉 depth
        drawList.addImageQuad(textureId,
                bX, bY, rX, rY, rX, rY + depth, bX, bY + depth,
                u0, v0, u1, v0, u1, v1, u0, v1,
                FACE_RIGHT);

        // 占位，让后续控件跳过这块区域
        imgui.ImGui.dummy(size, size);
    }

    private static String translate(String labelKey) {
        return Text.translate(labelKey).getString();
    }

}
