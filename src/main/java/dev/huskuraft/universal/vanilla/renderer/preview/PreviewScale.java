package dev.huskuraft.universal.vanilla.renderer.preview;

import dev.huskuraft.universal.api.renderer.PreviewBlock;
import dev.huskuraft.universal.api.renderer.PreviewCamera;
import dev.huskuraft.universal.api.renderer.PreviewOrientation;

import java.util.List;

/**
 * 预览的缩放系数计算。
 * <p>
 * 画中画通道的 {@code prepare} 里做的是：
 * <pre>
 *   int  width  = (x1 - x0) * guiScale;      // 离屏纹理的物理像素宽
 *   int  height = (y1 - y0) * guiScale;      // 离屏纹理的物理像素高
 *   pose.translate(width / 2f, getTranslateY(height, guiScale), 0f);
 *   pose.scale(guiScale * state.scale(), ...);   // ← 这里用了缩放系数
 * </pre>
 * 而正交投影的视锥是 {@code X∈[0,width], Y∈[0,height]}（物理像素），
 * 也就是说<b>视口里能看到的模型单位数 = 视口逻辑宽 / 缩放系数</b>
 * （guiScale 在分子分母里抵消掉了）。
 * <p>
 * 所以要让结构铺满视口，缩放系数要取：
 * <pre>
 *   0.9 × min(视口逻辑宽 / 投影宽, 视口逻辑高 / 投影高)
 * </pre>
 * 投影宽高按<b>旋转后</b>的结构包围盒算（以结构中心为原点），
 * 这样不论什么朝向都能填满视口。
 */
public final class PreviewScale {

    private PreviewScale() {
    }

    /**
     * 算出缩放系数。
     *
     * @param blocks         要画的方块
     * @param camera         相机
     * @param orientation    朝向修正
     * @param viewportWidth  视口宽（逻辑像素）
     * @param viewportHeight 视口高（逻辑像素）
     */
    public static float fit(List<PreviewBlock> blocks, PreviewCamera camera, PreviewOrientation orientation,
                             int viewportWidth, int viewportHeight) {
        if (blocks == null || blocks.isEmpty()) {
            return 1.0F;
        }
        var bounds = rotatedBounds(blocks, camera, orientation);
        var projectedWidth = Math.max(0.001F, bounds[1] - bounds[0]);
        var projectedHeight = Math.max(0.001F, bounds[3] - bounds[2]);

        // 视口（逻辑像素）
        var width = Math.max(1.0F, viewportWidth);
        var height = Math.max(1.0F, viewportHeight);
        var fit = Math.min(width / projectedWidth, height / projectedHeight);
        return Math.max(0.001F, 0.9F * fit * camera.zoom());
    }

    /**
     * 旋转后的结构在<b>视图 Z 轴</b>上的厚度（方块单位）。
     * <p>
     * 正交视锥的深度范围是有限的（画中画通道写死 ±1000），模型在视图 Z 上占多宽
     * 决定了会不会被近 / 远裁剪面削掉。高分辨率导出时模型在视图空间里被放得很大，
     * 深度也跟着变大 —— 所以导出要用这个厚度来把深度范围算够。
     */
    public static float depthExtent(List<PreviewBlock> blocks, PreviewCamera camera, PreviewOrientation orientation) {
        if (blocks == null || blocks.isEmpty()) {
            return 1.0F;
        }
        var bounds = rotatedBounds(blocks, camera, orientation);
        return Math.max(0.001F, bounds[5] - bounds[4]);
    }

    /**
     * 旋转后结构的包围盒（视图坐标系，以结构中心为原点）。
     *
     * @return {@code [minX, maxX, minY, maxY, minZ, maxZ]}
     */
    private static float[] rotatedBounds(List<PreviewBlock> blocks, PreviewCamera camera, PreviewOrientation orientation) {
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

        float minPX = Float.MAX_VALUE, minPY = Float.MAX_VALUE, minPZ = Float.MAX_VALUE;
        float maxPX = -Float.MAX_VALUE, maxPY = -Float.MAX_VALUE, maxPZ = -Float.MAX_VALUE;
        for (var block : blocks) {
            var position = block.position();
            for (int corner = 0; corner < 8; corner++) {
                double wx = position.x() + ((corner & 1) == 0 ? 0 : 1) - centerX;
                double wy = position.y() + ((corner & 2) == 0 ? 0 : 1) - centerY;
                double wz = position.z() + ((corner & 4) == 0 ? 0 : 1) - centerZ;
                // 第 0 行 → 视图的 X，第 1 行 → 视图的 Y，第 2 行 → 视图的 Z
                double rx = m[0] * wx + m[1] * wy + m[2] * wz;
                double ry = m[3] * wx + m[4] * wy + m[5] * wz;
                double rz = m[6] * wx + m[7] * wy + m[8] * wz;
                minPX = Math.min(minPX, (float) rx);
                maxPX = Math.max(maxPX, (float) rx);
                minPY = Math.min(minPY, (float) ry);
                maxPY = Math.max(maxPY, (float) ry);
                minPZ = Math.min(minPZ, (float) rz);
                maxPZ = Math.max(maxPZ, (float) rz);
            }
        }
        return new float[]{minPX, maxPX, minPY, maxPY, minPZ, maxPZ};
    }
}
