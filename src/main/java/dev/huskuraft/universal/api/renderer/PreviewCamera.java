package dev.huskuraft.universal.api.renderer;

/**
 * 模型预览的相机。
 *
 * @param yaw   绕 Y 轴的偏航角（度）
 * @param pitch 绕 X 轴的俯仰角（度，限制在 -90 ~ 90）
 * @param zoom  缩放（1 = 正好铺满视口）
 * @param panX  水平平移（逻辑像素）
 * @param panY  垂直平移（逻辑像素）
 */
public record PreviewCamera(float yaw, float pitch, float zoom, float panX, float panY) {
}
