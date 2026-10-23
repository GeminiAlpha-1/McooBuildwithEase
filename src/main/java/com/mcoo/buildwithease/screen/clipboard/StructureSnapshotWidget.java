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
 * 这一层只负责「要看什么」和「怎么操作」：把结构里的方块连同方块实体的数据交给平台层，
 * 再把相机参数（偏航 / 俯仰 / 缩放 / 平移）一起传过去。
 * 怎么画是平台层的事 —— 平台层用原版的 3D 管线渲染，
 * 所以方块实体（钟、箱子、陶罐、旗帜…）的形状才是对的。
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
    /** 默认偏航角（度）。 */
    private static final float DEFAULT_YAW = 45.0F;
    /** 默认俯仰角（度）。 */
    private static final float DEFAULT_PITCH = 30.0F;

    private Snapshot snapshot;
    /**
     * 要预览的方块列表。
     * <p>
     * 只在结构变化时重建 —— 每帧重建会白白分配一堆对象。
     */
    private java.util.List<PreviewBlock> blocks;
    /** 方块列表对应的结构指纹，用来判断是否需要重建。 */
    private int blocksFingerprint;
    /** 相机：偏航角（度）。 */
    private float yaw = DEFAULT_YAW;
    /** 相机：俯仰角（度）。 */
    private float pitch = DEFAULT_PITCH;
    /** 相机：缩放（1 = 正好铺满视口）。 */
    private float zoom = 1.0F;
    /** 相机：水平平移（逻辑像素）。 */
    private float panX = 0.0F;
    /** 相机：垂直平移（逻辑像素）。 */
    private float panY = 0.0F;
    /** 朝向修正：绕世界 X/Y/Z 轴各转多少个 90°（0 = 默认朝向）。 */
    private dev.huskuraft.universal.api.renderer.PreviewOrientation orientation =
            dev.huskuraft.universal.api.renderer.PreviewOrientation.DEFAULT;
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
        // 方块列表只在结构变化时重建（每帧重建会白白分配一堆对象）
        var fingerprint = fingerprintOf(snapshot);
        if (blocks == null || blocksFingerprint != fingerprint) {
            blocks = snapshot.blockData().stream()
                    .filter(blockData -> blockData.blockState() != null)
                    .map(blockData -> new PreviewBlock(blockData.blockPosition(), blockData.blockState(), blockData.entityTag()))
                    .toList();
            blocksFingerprint = fingerprint;
        }
        if (blocks.isEmpty()) {
            return;
        }

        // 平台层用 3D 管线渲染（离屏纹理 → 贴回这个矩形），
        // 这样方块实体（钟、箱子、陶罐、旗帜…）才能被正确画出来。
        renderer.renderModelPreview(
                new dev.huskuraft.universal.api.renderer.PreviewRenderRequest(
                        blocks, new PreviewCamera(yaw, pitch, zoom, panX, panY), orientation),
                getLeft(), getTop(), getWidth(), getHeight());
    }

    /**
     * 绕世界某根轴把结构转 90°。
     *
     * @param axis  0=X，1=Y，2=Z
     * @param steps 转多少个 90°（正数按右手定则）
     */
    public void rotateAxis(int axis, int steps) {
        this.orientation = this.orientation.rotate(axis, steps);
    }

    /** 回到默认视角：偏航 45°、俯仰 30°、缩放 100%、不平移、没有朝向修正。 */
    public void resetView() {
        this.yaw = DEFAULT_YAW;
        this.pitch = DEFAULT_PITCH;
        this.zoom = 1.0F;
        this.panX = 0.0F;
        this.panY = 0.0F;
        this.orientation = dev.huskuraft.universal.api.renderer.PreviewOrientation.DEFAULT;
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
