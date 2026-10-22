package com.mcoo.buildwithease.screen.clipboard;

import dev.huskuraft.universal.api.gui.AbstractWidget;
import dev.huskuraft.universal.api.platform.Entrance;
import dev.huskuraft.universal.api.renderer.PreviewBlock;
import com.mcoo.buildwithease.building.clipboard.Snapshot;
import dev.huskuraft.universal.api.renderer.PreviewCamera;
import dev.huskuraft.universal.api.renderer.Renderer;
import dev.huskuraft.universal.api.text.Text;

/**
 * 剪贴板的结构预览：一个可以自由观察的 3D 模型。
 * <p>
 * 每一块都用它自己的方块模型（真实贴图）渲染，再按相机参数投影到屏幕上，
 * 相邻方块共用角点，所以没有缝、不会歪。
 * <p>
 * 交互：
 * <ul>
 *   <li><b>左键拖动</b>：旋转（偏航 / 俯仰）</li>
 *   <li><b>右键拖动</b>：平移模型</li>
 *   <li><b>滚轮</b>：缩放</li>
 * </ul>
 */
public final class StructureSnapshotWidget extends AbstractWidget {

    private static final float ROTATE_SPEED = 0.5F;
    private static final float ZOOM_SPEED = 0.15F;
    private static final float MIN_ZOOM = 0.2F;
    private static final float MAX_ZOOM = 8.0F;

    private Snapshot snapshot;
    /**
     * 剔除后的预览网格。
     * <p>
     * 剔除（相邻面遮挡、整块剔除）与相机无关，只取决于结构本身，
     * 所以网格只在结构变化时重建一次，之后每帧只做投影。
     */
    private dev.huskuraft.universal.api.renderer.PreviewMesh mesh;
    /** 网格对应的结构指纹，用来判断是否需要重建。 */
    private int meshFingerprint;
    /** 相机：偏航角（度）。 */
    private float yaw = 45.0F;
    /** 相机：俯仰角（度）。 */
    private float pitch = 30.0F;
    /** 相机：缩放（1 = 正好铺满视口）。 */
    private float zoom = 1.0F;
    /** 相机：水平平移（逻辑像素）。 */
    private float panX = 0.0F;
    /** 相机：垂直平移（逻辑像素）。 */
    private float panY = 0.0F;
    /** 鼠标是否还按着。 */
    private boolean dragging = false;

    public StructureSnapshotWidget(Entrance entrance, int x, int y, int width, int height, Snapshot snapshot) {
        super(entrance, x, y, width, height, Text.empty());
        this.snapshot = snapshot;
    }

    @Override
    public void onCreate() {
    }

    @Override
    public void renderWidget(Renderer renderer, int mouseX, int mouseY, float deltaTick) {
        if (snapshot == null || snapshot.isEmpty()) {
            return;
        }
        // 网格只在结构变化时重建（剔除结果与相机无关，可以缓存）
        var fingerprint = fingerprintOf(snapshot);
        if (mesh == null || meshFingerprint != fingerprint) {
            var blocks = snapshot.blockData().stream()
                    .filter(blockData -> blockData.blockState() != null)
                    .map(blockData -> new PreviewBlock(blockData.blockPosition(), blockData.blockState()))
                    .toList();
            mesh = renderer.buildPreviewMesh(blocks);
            meshFingerprint = fingerprint;
        }
        if (mesh.isEmpty()) {
            return;
        }

        // 裁剪由预览元素自己的 scissorArea() 负责（自己提交的 GUI 元素不吃全局裁剪），
        // 这里不要再 pushScissor —— 全局裁剪和元素裁剪叠加会把边界算错。
        renderer.renderModelPreview(mesh, new PreviewCamera(yaw, pitch, zoom, panX, panY),
                getLeft(), getTop(), getWidth(), getHeight());
    }

    /**
     * 结构的指纹：方块数 + 各方块坐标与状态的哈希。
     * <p>
     * 用来判断「结构有没有变」——变了才重建网格。
     * 坐标和状态都参与哈希，所以挪动一格或换一种方块都会触发重建。
     */
    private static int fingerprintOf(Snapshot snapshot) {
        var hash = 17;
        for (var blockData : snapshot.blockData()) {
            var position = blockData.blockPosition();
            hash = hash * 31 + position.x();
            hash = hash * 31 + position.y();
            hash = hash * 31 + position.z();
            hash = hash * 31 + (blockData.blockState() == null ? 0 : blockData.blockState().hashCode());
        }
        return hash;
    }

    @Override
    public boolean onMouseClicked(double mouseX, double mouseY, int button) {
        if (!isMouseOver(mouseX, mouseY)) {
            return false;
        }
        dragging = true;
        return true;
    }

    @Override
    public boolean onMouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        return true;
    }

    @Override
    public boolean onMouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (!dragging) {
            return false;
        }
        if (button == 0) {
            // 左键：旋转
            yaw -= (float) deltaX * ROTATE_SPEED;
            pitch = Math.max(-90.0F, Math.min(90.0F, pitch + (float) deltaY * ROTATE_SPEED));
        } else if (button == 1) {
            // 右键：平移
            panX += (float) deltaX;
            panY += (float) deltaY;
        }
        return true;
    }

    @Override
    public boolean onMouseScrolled(double mouseX, double mouseY, double amountX, double amountY) {
        if (!isMouseOver(mouseX, mouseY)) {
            return false;
        }
        // 滚轮向上（amountY > 0）放大，向下缩小 —— 与常见的 3D 视图操作习惯一致。
        zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom * (float) (1.0D + amountY * ZOOM_SPEED)));
        return true;
    }

    private int backgroundColor = 0xdc000000;

    @Override
    public void renderWidgetBackground(Renderer renderer, int mouseX, int mouseY, float deltaTick) {
        renderer.renderGradientRect(getLeft(), getTop(), getRight(), getBottom(), backgroundColor, backgroundColor);
    }

    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public void setSnapshot(Snapshot snapshot) {
        // 不在这里清空网格：onReload 每帧都会调到这里，
        // 是否需要重建由 renderWidget 里的结构指纹比对决定。
        this.snapshot = snapshot;
    }
}
