package dev.huskuraft.universal.vanilla.renderer.preview;

import java.util.List;

import dev.huskuraft.universal.api.renderer.PreviewBlock;
import dev.huskuraft.universal.api.renderer.PreviewCamera;
import dev.huskuraft.universal.api.renderer.PreviewOrientation;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;

/**
 * 「导出图片」用的 PiP（画中画）渲染状态。
 * <p>
 * 和 {@link PreviewRenderState} 是一回事，只是类型不同 ——
 * 画中画通道按「渲染状态类型」分派渲染器，预览和导出得用各自的离屏纹理。
 *
 * @param blocks      要渲染的方块
 * @param camera      相机
 * @param orientation 朝向修正
 * @param x0          视口左边
 * @param y0          视口上边
 * @param x1          视口右边
 * @param y1          视口下边
 * @param scissorArea 裁剪区
 * @param bounds      包围盒
 */
public record PreviewExportRenderState(
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

    @Override
    public float scale() {
        return PreviewScale.fit(blocks, camera, orientation, x1 - x0, y1 - y0);
    }
}
