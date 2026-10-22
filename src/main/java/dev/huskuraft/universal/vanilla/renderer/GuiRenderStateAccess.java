package dev.huskuraft.universal.vanilla.renderer;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiRenderState;

/**
 * 取到 26.2 的 {@link GuiRenderState}。
 * <p>
 * 26.2 的 GUI 是「状态机」：{@code GuiGraphicsExtractor} 把绘制指令写进 {@link GuiRenderState}，
 * 渲染器最后统一提交。{@code GuiGraphicsExtractor} 的 {@code guiRenderState} 字段是 private，
 * 原版也没有公开的 getter（{@code net.minecraft.client.gui.Gui} 里那个也是 private）。
 * <p>
 * 我们需要一个公开入口来提交<b>自定义</b>的 GUI 元素（任意四边形），
 * 所以这里用反射取一次。取不到就返回 null，调用方退回「仿射近似」的画法（会有一丝划痕，但不至于画不出来）。
 */
final class GuiRenderStateAccess {

    private static final java.lang.reflect.Field FIELD = find();

    private GuiRenderStateAccess() {
    }

    private static java.lang.reflect.Field find() {
        try {
            var field = GuiGraphicsExtractor.class.getDeclaredField("guiRenderState");
            field.setAccessible(true);
            return field;
        } catch (Throwable throwable) {
            return null;
        }
    }

    static boolean available() {
        return FIELD != null;
    }

    static GuiRenderState get(GuiGraphicsExtractor graphics) {
        if (FIELD == null || graphics == null) {
            return null;
        }
        try {
            return (GuiRenderState) FIELD.get(graphics);
        } catch (Throwable throwable) {
            return null;
        }
    }
}
