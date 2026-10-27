package dev.huskuraft.universal.api.gui;

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

}
