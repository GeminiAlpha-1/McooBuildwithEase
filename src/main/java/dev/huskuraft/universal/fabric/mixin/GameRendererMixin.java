package dev.huskuraft.universal.fabric.mixin;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.huskuraft.universal.vanilla.renderer.preview.PreviewExportRenderer;
import dev.huskuraft.universal.vanilla.renderer.preview.PreviewPictureInPictureRenderer;
import dev.huskuraft.universal.vanilla.renderer.preview.PreviewRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.GameRenderer;

/**
 * 让 GUI 的画中画（PiP）渲染器列表可以追加自定义实现。
 * <p>
 * 26.2 的 {@code GameRenderer} 构造 {@code GuiRenderer} 时，
 * PiP 渲染器列表是写死的 {@code List.of(...)}（五个原版实现），
 * 而 {@code GuiRenderer} 内部把它存成 {@code ImmutableMap} ——
 * 构造完就改不了了，想加自己的渲染器只能在这里动手。
 * <p>
 * 剪贴板的三维预览要用这个通道：方块实体（钟、箱子、陶罐、旗帜…）的形状
 * 不在方块状态模型里，只有走 3D 管线才能画出来。
 * <p>
 * 用 {@code @ModifyArg} 替换传给 {@code GuiRenderer} 构造器的第三个参数（PiP 列表），
 * 把预览渲染器追加进去 —— 比反射改 {@code final} 字段干净，也不碰原版的五个实现。
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @ModifyArg(method = "<init>", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/render/GuiRenderer;<init>(Lnet/minecraft/client/renderer/state/gui/GuiRenderState;Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher;Ljava/util/List;)V"),
            index = 2)
    private List<PictureInPictureRenderer<?>> mcoo$addPreviewRenderer(List<PictureInPictureRenderer<?>> original) {
        try {
            var list = new ArrayList<>(original);
            list.add(new PreviewPictureInPictureRenderer(Minecraft.getInstance()));
            // 「导出图片」也要占一个画中画渲染器：3D 内容必须在 GUI 渲染通道**里面**画，
            // 借它的时机把输出切到自己那张可回读的纹理上。
            var exportRenderer = new PreviewExportRenderer(Minecraft.getInstance());
            PreviewExportRenderer.register(exportRenderer);
            list.add(exportRenderer);
            return list;
        } catch (Throwable throwable) {
            // 拿不到就退回原版列表（预览不显示，但不影响其它功能）
            return original;
        }
    }
}
