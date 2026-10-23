package dev.huskuraft.universal.vanilla.renderer.preview;

import java.util.List;

import dev.huskuraft.universal.api.renderer.PreviewBlock;
import dev.huskuraft.universal.api.renderer.PreviewCamera;
import dev.huskuraft.universal.api.renderer.PreviewOrientation;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;

/**
 * 三维结构预览的 PiP（画中画）渲染状态。
 * <p>
 * 走 MC 原版的离屏渲染通道：{@code GuiRenderer} 会为它建一张离屏纹理，
 * 调用 {@link PreviewPictureInPictureRenderer#renderToTexture} 把 3D 内容画进去，
 * 再把纹理贴回 GUI 的对应矩形。
 *
 * @param blocks      要渲染的方块（含方块实体 NBT）
 * @param camera      相机
 * @param orientation 朝向修正（绕世界轴各转多少个 90°）
 * @param x0          视口左边
 * @param y0          视口上边
 * @param x1          视口右边
 * @param y1          视口下边
 * @param scissorArea 裁剪区
 * @param bounds      包围盒
 */
public record PreviewRenderState(
        List<PreviewBlock> blocks,
        PreviewCamera camera,
        PreviewOrientation orientation,
        int x0,
        int y0,
        int x1,
        int y1,
        ScreenRectangle scissorArea,
        ScreenRectangle bounds
) implements PictureInPictureRenderState {

    /**
     * 缩放系数。
     * <p>
     * 详细算法见 {@link PreviewScale} —— 预览和「导出图片」用的是同一套，
     * 两边的画面才会一模一样。
     */
    @Override
    public float scale() {
        return PreviewScale.fit(blocks, camera, orientation, x1 - x0, y1 - y0);
    }
}
