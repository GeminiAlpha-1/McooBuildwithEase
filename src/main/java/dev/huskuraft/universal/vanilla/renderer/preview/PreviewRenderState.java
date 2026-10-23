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
 * @param scissor     裁剪区
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
     * 【缩放只在这里做一次】{@code PictureInPictureRenderer#prepare} 里做的是：
     * <pre>
     *   int  width  = (x1 - x0) * guiScale;      // 离屏纹理的物理像素宽
     *   int  height = (y1 - y0) * guiScale;      // 离屏纹理的物理像素高
     *   pose.translate(width / 2f, getTranslateY(height, guiScale), 0f);
     *   pose.scale(guiScale * state.scale(), ...);   // ← 这里用了本方法的结果
     * </pre>
     * 而正交投影的视锥是 {@code X∈[0,width], Y∈[0,height]}（物理像素），
     * 也就是说<b>视口里能看到的模型单位数 = 视口逻辑宽 / state.scale()</b>
     * （guiScale 在分子分母里抵消掉了）。
     * <p>
     * 所以要让结构铺满视口，本方法要返回：
     * <pre>
     *   scale = 0.9 × min(视口逻辑宽 / 投影宽, 视口逻辑高 / 投影高)
     * </pre>
     * 投影宽高按<b>旋转后</b>的结构包围盒算（以结构中心为原点），
     * 这样不论什么朝向都能填满视口。
     */
    @Override
    public float scale() {
        if (blocks == null || blocks.isEmpty()) {
            return 1.0F;
        }

        // 结构包围盒（方块单位）
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        for (var block : blocks) {
            var position = block.position();
            minX = Math.min(minX, position.x());
            maxX = Math.max(maxX, position.x());
            minY = Math.min(minY, position.y());
            maxY = Math.max(maxY, position.y());
            minZ = Math.min(minZ, position.z());
            maxZ = Math.max(maxZ, position.z());
        }
        double centerX = (minX + maxX + 1) / 2.0;
        double centerY = (minY + maxY + 1) / 2.0;
        double centerZ = (minZ + maxZ + 1) / 2.0;

        // 旋转矩阵（和渲染那边共用同一份，角度不一致会导致缩放算错）
        var m = PreviewRotation.matrix(camera, orientation);

        float minPX = Float.MAX_VALUE, minPY = Float.MAX_VALUE;
        float maxPX = -Float.MAX_VALUE, maxPY = -Float.MAX_VALUE;
        for (var block : blocks) {
            var position = block.position();
            for (int corner = 0; corner < 8; corner++) {
                double wx = position.x() + ((corner & 1) == 0 ? 0 : 1) - centerX;
                double wy = position.y() + ((corner & 2) == 0 ? 0 : 1) - centerY;
                double wz = position.z() + ((corner & 4) == 0 ? 0 : 1) - centerZ;
                // 第 0 行 → 视图的 X，第 1 行 → 视图的 Y
                double rx = m[0] * wx + m[1] * wy + m[2] * wz;
                double ry = m[3] * wx + m[4] * wy + m[5] * wz;
                minPX = Math.min(minPX, (float) rx);
                maxPX = Math.max(maxPX, (float) rx);
                minPY = Math.min(minPY, (float) ry);
                maxPY = Math.max(maxPY, (float) ry);
            }
        }
        var projectedWidth = Math.max(0.001F, maxPX - minPX);
        var projectedHeight = Math.max(0.001F, maxPY - minPY);

        // 视口（逻辑像素）
        var viewportWidth = Math.max(1.0F, x1 - x0);
        var viewportHeight = Math.max(1.0F, y1 - y0);
        var fit = Math.min(viewportWidth / projectedWidth, viewportHeight / projectedHeight);
        return Math.max(0.001F, 0.9F * fit * camera.zoom());
    }
}
