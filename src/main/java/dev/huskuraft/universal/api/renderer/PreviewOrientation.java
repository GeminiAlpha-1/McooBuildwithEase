package dev.huskuraft.universal.api.renderer;

/**
 * 预览的朝向修正。
 * <p>
 * 用来在相机的偏航 / 俯仰之外，再把结构整体绕世界的某根轴转 90° ——
 * 比如「绕 X 轴旋转 90°」把一座房子横过来看。
 *
 * @param x 绕世界 X 轴转过的 90° 步数（正数按右手定则，即从 +X 看向原点时为逆时针）
 * @param y 绕世界 Y 轴转过的 90° 步数
 * @param z 绕世界 Z 轴转过的 90° 步数
 */
public record PreviewOrientation(int x, int y, int z) {

    /** 没有修正（默认朝向）。 */
    public static final PreviewOrientation DEFAULT = new PreviewOrientation(0, 0, 0);

    /**
     * 绕某根轴再转 {@code steps} 个 90°。
     *
     * @param axis  0=X，1=Y，2=Z
     * @param steps 步数，正数按右手定则
     * @return 新的朝向
     */
    public PreviewOrientation rotate(int axis, int steps) {
        return switch (axis) {
            case 0 -> new PreviewOrientation(x + steps, y, z);
            case 1 -> new PreviewOrientation(x, y + steps, z);
            case 2 -> new PreviewOrientation(x, y, z + steps);
            default -> this;
        };
    }
}
