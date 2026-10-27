package dev.huskuraft.universal.vanilla.gui;

import cn.enaium.fabric.imgui.ImGuiRenderable;
import imgui.ImGuiIO;

import dev.huskuraft.universal.api.gui.ImGuiScreen;

/**
 * 用 ImGui 绘制的 MC 界面。
 * <p>
 * ImGui 库（fabric-gui-imgui）给 {@code GameRenderer#render} 打了 mixin：
 * <b>当前界面只要实现了它的 {@link ImGuiRenderable}，就会自动走 ImGui 绘制</b>。
 * 所以我们只要把业务层的 {@link ImGuiScreen} 包成这个类即可 ——
 * vanilla 那套控件（按钮 / 列表 / 文本框）一个都不用画。
 * <p>
 * 继承 {@link MinecraftProxyScreen} 的原因：父类已经把 api 层的 {@code Screen}
 * 适配成了 MC 界面，继承它就不用再写一遍事件转发，而且
 * {@code MinecraftClient#getPanel()} 认得它（界面栈不会错乱）。
 * <p>
 * ⚠️ 本类引用了 ImGui 库的类 → <b>没装库时不能加载本类</b>。
 * 所以创建它的地方必须先判 {@code ImGuiSupport#isAvailable()}。
 */
public final class MinecraftImGuiProxyScreen extends MinecraftProxyScreen implements ImGuiRenderable {

    public MinecraftImGuiProxyScreen(ImGuiScreen proxy) {
        super(proxy);
    }

    /** ImGui 宿主调这个：所有界面内容都在这里用 ImGui 画。 */
    @Override
    public void render(ImGuiIO io) {
        ((ImGuiScreen) getProxy()).renderImGui();
    }

    /**
     * 界面内容由 ImGui 画，vanilla 这条路不画控件 ——
     * 但<b>三维预览</b>必须走这里：ImGui 只知道 2D，方块实体（箱子 / 钟 / 旗帜…）
     * 的形状只有 MC 的 3D 管线才画得出来。
     *
     * <p>流程：界面给出「要预览什么 + 占多大矩形」→ 我们把它作为画中画（PiP）状态提交给
     * {@code GuiRenderState} → 原版 {@code GuiRenderer} 为该矩形建离屏纹理，
     * 用 3D 管线把结构画进去，并把纹理 view 发布到 {@code PreviewTexture} →
     * 界面在 ImGui 里按同一个矩形采样那张纹理。
     *
     * <p>⚠️ 原版会把纹理**贴回**这个矩形（那是它自己的 PiP 流程）。
     * ImGui 面板随后绘制自己的底色 + 图像，所以玩家不会看到两层叠加 ——
     * 代价是那一次贴图白做，但换来的是完全不碰原版渲染管线。
     */
    @Override
    public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        var screen = (ImGuiScreen) getProxy();
        var request = screen.nativePreview();
        var bounds = screen.nativePreviewBounds();
        if (request == null || request.blocks() == null || request.blocks().isEmpty() || bounds == null || bounds.length < 4) {
            return;
        }
        int x = bounds[0];
        int y = bounds[1];
        int width = bounds[2];
        int height = bounds[3];
        if (width <= 0 || height <= 0) {
            return;
        }

        var minecraft = net.minecraft.client.Minecraft.getInstance();
        var guiRenderState = minecraft.gameRenderer.gameRenderState().guiRenderState;
        var scissor = new net.minecraft.client.gui.navigation.ScreenRectangle(x, y, width, height);
        guiRenderState.addPicturesInPictureState(
                new dev.huskuraft.universal.vanilla.renderer.preview.PreviewRenderState(
                        request.blocks(),
                        request.camera(),
                        request.orientation(),
                        x, y, x + width, y + height,
                        scissor,
                        net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState.getBounds(
                                x, y, x + width, y + height, scissor)));
    }

}
