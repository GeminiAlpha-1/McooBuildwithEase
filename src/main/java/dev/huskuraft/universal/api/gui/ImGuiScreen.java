package dev.huskuraft.universal.api.gui;

import dev.huskuraft.universal.api.renderer.PreviewRenderRequest;

/**
 * 用 <b>Dear ImGui</b> 绘制的界面。
 * <p>
 * 本 mod 自己的 GUI 框架功能太少，所以二级 / 三级菜单都改用 ImGui 画
 * （按 Alt 打开的建造编辑界面除外，那个仍然用自研 GUI）。
 * <p>
 * <b>怎么工作的</b>：平台的 ImGui 宿主会在渲染阶段检查当前界面是不是实现了本接口，
 * 是的话就调 {@link #renderImGui()}，我们在这里直接调 {@code imgui.ImGui.*} 画控件。
 * <p>
 * <b>可用性</b>：ImGui 库（fabric-gui-imgui）没装时，这些界面会被
 * {@link ImGuiSupport#isAvailable()} 挡掉 —— 直接隐藏，不崩。
 * 本接口本身不引用任何 ImGui 的类，所以没装库也不会影响别的界面。
 */
public interface ImGuiScreen extends Screen {

    /**
     * 每帧绘制。
     * <p>
     * 业务层在这里直接调 {@code imgui.ImGui.*}（编译期依赖是 compileOnly，
     * 运行期由 ImGui 库提供这些类）。
     */
    void renderImGui();

    /**
     * 本帧需要 <b>MC 原生渲染</b>的三维预览（结构预览这类）。
     * <p>
     * ImGui 只画 2D，画不了方块实体（箱子 / 钟 / 旗帜…）。所以 ThreeD 预览走平台层的
     * 画中画（PiP）：平台把预览渲染进离屏纹理，界面再按
     * {@link #nativePreviewBounds()} 的矩形把纹理画出来。
     * <p>
     * ⚠️ 这个方法和 {@link #renderImGui()} 在同一帧里被调用，但<b>先后顺序不保证</b> ——
     * 纹理内容可能比拖动晚一帧。所以别在这里做「必须与输入同帧」的判断。
     *
     * @return {@code null} = 本界面不需要原生预览
     */
    default PreviewRenderRequest nativePreview() {
        return null;
    }

    /**
     * 原生预览占用的屏幕矩形，{@code {x, y, width, height}}（GUI 逻辑像素）。
     * <p>
     * 只有在 {@link #nativePreview()} 返回非 null 时才有意义。
     * 返回 {@code null} 或尺寸 ≤ 0 时平台层不会渲染。
     */
    default int[] nativePreviewBounds() {
        return null;
    }

}
