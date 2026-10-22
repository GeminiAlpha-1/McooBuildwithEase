package dev.huskuraft.universal.vanilla.gui;

import com.mojang.blaze3d.platform.InputConstants;

import dev.huskuraft.universal.api.gui.Screen;
import dev.huskuraft.universal.vanilla.renderer.MinecraftRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class MinecraftProxyScreen extends net.minecraft.client.gui.screens.Screen {

    private final Screen proxy;

    public MinecraftProxyScreen(Screen screen) {
        super(screen.getScreenTitle().reference());
        this.proxy = screen;
    }

    public Screen getProxy() {
        return proxy;
    }

    @Override
    public void tick() {
        proxy.onTick();
    }

    @Override
    protected void init() {
        proxy.init(width, height);
    }

    @Override
    protected void rebuildWidgets() {
        proxy.init(width, height);
    }

    @Override
    public void removed() {
        proxy.onDestroy();
    }

    @Override
    public void onClose() {
    }

    // 26.2 起 Screen 不再直接绘制，而是把绘制指令写进渲染状态，render(GuiGraphics, ...) 改名为 extractRenderState(...)
    @Override
    public void extractRenderState(GuiGraphicsExtractor minecraftRenderer, int i, int j, float f) {
        // 26.2 的 GuiGraphicsExtractor 自带 2D 矩阵（pose() 返回 Matrix3x2fStack），渲染适配层直接接管它
        var renderer = new MinecraftRenderer(minecraftRenderer);
        proxy.render(renderer, i, j, f);
        proxy.renderOverlay(renderer, i, j, f);
    }

    @Override
    public boolean isPauseScreen() {
        return proxy.isPauseGame();
    }

    @Override
    public void mouseMoved(double d, double e) {
        proxy.onMouseMoved(d, e);
    }

    // 26.2 把鼠标/键盘事件改成了 record（MouseButtonEvent / KeyEvent / CharacterEvent），不再传裸 int
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (dev.huskuraft.universal.vanilla.renderer.MinecraftBufferSource.DEBUG) {
            org.slf4j.LoggerFactory.getLogger(MinecraftProxyScreen.class)
                    .info("[gui] mouseClicked at ({},{}) button={}", event.x(), event.y(), event.button());
        }
        return proxy.onMouseClicked(event.x(), event.y(), event.button());
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return proxy.onMouseReleased(event.x(), event.y(), event.button());
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double f, double g) {
        return proxy.onMouseDragged(event.x(), event.y(), event.button(), f, g);
    }

    @Override
    public boolean mouseScrolled(double d, double e, double f, double g) {
        return proxy.onMouseScrolled(d, e, f, g);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return proxy.onKeyPressed(event.key(), event.scancode(), event.modifiers());
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        return proxy.onKeyReleased(event.key(), event.scancode(), event.modifiers());
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return proxy.onCharTyped((char) event.codepoint(), currentModifiers());
    }

    //    @Override
    @Deprecated
    public boolean changeFocus(boolean bl) {
        return proxy.onFocusMove(bl);
    }

    @Override
    public boolean isMouseOver(double d, double e) {
        return proxy.isMouseOver(d, e);
    }

    @Override
    public Component getTitle() {
        return proxy.getScreenTitle().reference();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof MinecraftProxyScreen keyBinding && proxy.equals(keyBinding.proxy);
    }

    @Override
    public int hashCode() {
        return proxy.hashCode();
    }

    // 26.2 的 CharacterEvent 只带码点，不带修饰键，这里按当前按键状态补出 GLFW 风格的修饰键掩码
    private static int currentModifiers() {
        var minecraft = Minecraft.getInstance();
        var modifiers = 0;
        if (minecraft.hasShiftDown()) modifiers |= InputConstants.MOD_SHIFT;
        if (minecraft.hasControlDown()) modifiers |= InputConstants.MOD_CONTROL;
        if (minecraft.hasAltDown()) modifiers |= InputConstants.MOD_ALT;
        return modifiers;
    }
}
