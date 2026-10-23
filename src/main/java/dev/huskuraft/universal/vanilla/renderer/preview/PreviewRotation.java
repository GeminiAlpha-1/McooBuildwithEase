package dev.huskuraft.universal.vanilla.renderer.preview;

import dev.huskuraft.universal.api.renderer.PreviewCamera;
import dev.huskuraft.universal.api.renderer.PreviewOrientation;
import org.joml.Matrix3f;
import org.joml.Quaternionf;

/**
 * 预览相机的旋转。
 * <p>
 * 总旋转 = <b>绕世界轴的 90° 步进</b> × <b>Rx(180° + 俯仰) × Ry(偏航)</b>。
 * <p>
 * 【为什么 X 轴要转 180° + 俯仰】
 * 画中画（PiP）离屏通道的正交投影把视图的 +Y 映到了图像的下方，
 * Z 也被 {@code prepare} 里的 {@code scale(s, s, -s)} 取了反 —— 等效于相机站在了模型背后。
 * 把模型绕 X 轴翻 180° 之后，默认视角才是「相机在前方偏左上方」的三分之四视角：
 * 正面、左面、顶面同时可见，旋转时竖直轴也始终保持竖直。
 * <p>
 * 【为什么缩放和渲染要共用这份矩阵】
 * 缩放系数是按「旋转后结构的投影包围盒」算的，
 * 一旦这里和渲染那边用的角度不一致，结构就会忽大忽小或者被裁掉。
 * 所以两边都从这里取。
 */
public final class PreviewRotation {

    private PreviewRotation() {
    }

    /**
     * 完整旋转（相机 + 朝向修正）。
     *
     * @param camera      相机（偏航 / 俯仰）
     * @param orientation 朝向修正（绕世界轴各转多少个 90°）
     */
    public static Quaternionf of(PreviewCamera camera, PreviewOrientation orientation) {
        // 偏航再加 180°：默认视角要看到结构的**正面**（旗子的布面、箱子的插销）而不是背面，
        // 露出的侧面才是站在正面看时结构的**左面**。
        var base = new Quaternionf().rotateXYZ(
                (float) Math.toRadians(180.0 + camera.pitch()),
                (float) Math.toRadians(camera.yaw()) + (float) Math.PI,
                0.0F);
        if (orientation == null) {
            return base;
        }
        var axis = new Quaternionf().rotateXYZ(
                orientation.x() * 90.0F,
                orientation.y() * 90.0F,
                orientation.z() * 90.0F);
        return base.mul(axis);
    }

    /**
     * 完整旋转的 3×3 矩阵，<b>行主序</b>：{@code m[row * 3 + col]}。
     * <p>
     * 顶点变换：{@code x' = m[0]*x + m[1]*y + m[2]*z}（第 0 行），
     * {@code y' = m[3]*x + m[4]*y + m[5]*z}（第 1 行）。
     */
    public static float[] matrix(PreviewCamera camera, PreviewOrientation orientation) {
        var source = new Matrix3f();
        of(camera, orientation).get(source);
        var rows = new float[9];
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                // JOML 的 Matrix3f 是列主序存的：get(列, 行)
                rows[row * 3 + column] = source.get(column, row);
            }
        }
        return rows;
    }
}
