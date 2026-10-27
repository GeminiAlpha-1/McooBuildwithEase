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

    /** 界面内容由 ImGui 画，vanilla 这条路不画任何东西。 */
    @Override
    public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
    }

}
