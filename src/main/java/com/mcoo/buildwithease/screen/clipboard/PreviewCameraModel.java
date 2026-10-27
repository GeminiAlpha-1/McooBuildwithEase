package com.mcoo.buildwithease.screen.clipboard;

import dev.huskuraft.universal.api.renderer.PreviewCamera;
import dev.huskuraft.universal.api.renderer.PreviewOrientation;

/**
 * 结构预览的<b>视角状态</b>：偏航 / 俯仰 / 缩放 / 平移 / 朝向修正 —— 以及改变它们的操作。
 *
 * <h3>为什么要单独一个类</h3>
 * 同一套视角要被两个地方共用：自研 GUI 的预览控件（{@code StructureSnapshotWidget}）
 * 和 ImGui 里的剪贴板面板。视角状态和「怎么响应鼠标」是一件事，复制两份必然会走偏
 * （之前就踩过：右拖模型左转）。所以状态和操作都收敛在这里，两个界面只负责把输入喂进来。
 *
 * <p>这个类只依赖 common-api 的相机类型，<b>零 MC 依赖</b>，也不碰任何绘制。
 */
public final class PreviewCameraModel {

    /** 转得多快（度 / 逻辑像素）。 */
    public static final float ROTATE_SPEED = 0.5F;
    /** 滚一格缩放多少。 */
    public static final float ZOOM_SPEED = 0.15F;
    public static final float MIN_ZOOM = 0.2F;
    public static final float MAX_ZOOM = 8.0F;
    /** 默认偏航角（度）。 */
    public static final float DEFAULT_YAW = 45.0F;
    /** 默认俯仰角（度）。 */
    public static final float DEFAULT_PITCH = 30.0F;

    private float yaw = DEFAULT_YAW;
    private float pitch = DEFAULT_PITCH;
    private float zoom = 1.0F;
    private float panX;
    private float panY;
    private PreviewOrientation orientation = PreviewOrientation.DEFAULT;

    // ---- 滑条用的读写（微调：拖动旋转不可能精确到 1°） ----

    public float yaw() {
        return yaw;
    }

    public void setYaw(float value) {
        yaw = value;
    }

    public float pitch() {
        return pitch;
    }

    public void setPitch(float value) {
        pitch = Math.max(-90.0F, Math.min(90.0F, value));
    }

    public float zoom() {
        return zoom;
    }

    public void setZoom(float value) {
        zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, value));
    }
    /** 交给平台层渲染用的相机。 */
    public PreviewCamera camera() {
        return new PreviewCamera(yaw, pitch, zoom, panX, panY);
    }

    public PreviewOrientation orientation() {
        return orientation;
    }

    /**
     * 左键拖动：旋转。
     * <p>
     * <b>方向</b>：模型要跟着鼠标走 —— 往右拖，模型就往右转。
     * （之前这里是减号，方向正好相反。）
     */
    public void rotateByDrag(double deltaX, double deltaY) {
        yaw += (float) deltaX * ROTATE_SPEED;
        pitch = Math.max(-90.0F, Math.min(90.0F, pitch + (float) deltaY * ROTATE_SPEED));
    }

    /** 右键拖动：平移（逻辑像素）。 */
    public void panByDrag(double deltaX, double deltaY) {
        panX += (float) deltaX;
        panY += (float) deltaY;
    }

    /** 滚轮：向上放大、向下缩小（和常见的 3D 视图操作一致）。 */
    public void zoomByScroll(double amount) {
        zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom * (float) (1.0D + amount * ZOOM_SPEED)));
    }

    /**
     * 绕世界某根轴把结构转 90°。
     *
     * @param axis  0=X，1=Y，2=Z
     * @param steps 转多少个 90°（正数按右手定则）
     */
    public void rotateAxis(int axis, int steps) {
        orientation = orientation.rotate(axis, steps);
    }

    /** 回到默认视角：偏航 45°、俯仰 30°、缩放 100%、不平移、没有朝向修正。 */
    public void reset() {
        yaw = DEFAULT_YAW;
        pitch = DEFAULT_PITCH;
        zoom = 1.0F;
        panX = 0.0F;
        panY = 0.0F;
        orientation = PreviewOrientation.DEFAULT;
    }

    /**
     * 视角快照。
     * <p>
     * 自研界面的控件会在 {@code init → recreate} 时被重建，
     * 视角参数得由外层存着、重建后塞回去，否则「转了轴回到上一页发现什么都没变」。
     */
    public record ViewState(
            float yaw,
            float pitch,
            float zoom,
            float panX,
            float panY,
            PreviewOrientation orientation
    ) {
    }

    public ViewState viewState() {
        return new ViewState(yaw, pitch, zoom, panX, panY, orientation);
    }

    public void setViewState(ViewState state) {
        if (state == null) {
            return;
        }
        yaw = state.yaw();
        pitch = state.pitch();
        zoom = state.zoom();
        panX = state.panX();
        panY = state.panY();
        orientation = state.orientation();
    }

}
