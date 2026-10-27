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

    private Snapshot snapshot;
    /**
     * 要预览的方块列表。
     * <p>
     * 只在结构变化时重建 —— 每帧重建会白白分配一堆对象。
     */
    private java.util.List<PreviewBlock> blocks;
    /** 方块列表对应的结构指纹，用来判断是否需要重建。 */
    private int blocksFingerprint;
    /** 视角状态（和 ImGui 剪贴板面板共用同一套模型，见 {@link PreviewCameraModel}）。 */
    private final PreviewCameraModel cameraModel = new PreviewCameraModel();
    /** 鼠标是否还按着。 */
    private boolean dragging = false;
    /** 上一帧用的渲染器（导出图片时要用）。 */
    private dev.huskuraft.universal.api.renderer.Renderer renderer;
    /** 排队等导出的图片宽（0 = 没有）。 */
    private int exportWidth;
    /** 排队等导出的图片高。 */
    private int exportHeight;

    public StructureSnapshotWidget(Entrance entrance, int x, int y, int width, int height, Snapshot snapshot) {
        super(entrance, x, y, width, height, Text.empty());
        this.snapshot = snapshot;
    }

    @Override
    public void onCreate() {
    }

    @Override
    public void renderWidget(Renderer renderer, int mouseX, int mouseY, float deltaTick) {
        this.renderer = renderer;
        if (snapshot == null || snapshot.isEmpty()) {
            return;
        }
        // 方块列表只在结构变化时重建（每帧重建会白白分配一堆对象）
        var fingerprint = PreviewSource.fingerprintOf(snapshot);
        if (blocks == null || blocksFingerprint != fingerprint) {
            blocks = PreviewSource.blocksOf(snapshot);
            blocksFingerprint = fingerprint;
        }
        if (blocks.isEmpty()) {
            return;
        }

        // 排队等着的导出：这一帧渲染器和方块列表都齐了，可以导了
        if (this.exportWidth > 0) {
            var width = this.exportWidth;
            var height = this.exportHeight;
            this.exportWidth = 0;
            this.exportHeight = 0;
            exportImage(width, height);
        }

        // 平台层用 3D 管线渲染（离屏纹理 → 贴回这个矩形），
        // 这样方块实体（钟、箱子、陶罐、旗帜…）才能被正确画出来。
        renderer.renderModelPreview(
                new dev.huskuraft.universal.api.renderer.PreviewRenderRequest(
                        blocks, cameraModel.camera(), cameraModel.orientation()),
                getLeft(), getTop(), getWidth(), getHeight());
    }

    /**
     * 预览的视角状态（实现见 {@link PreviewCameraModel}，和 ImGui 剪贴板面板共用同一份模型）。
     * <p>
     * 界面每次重新初始化都会把控件重建一遍（{@code init → recreate → onCreate}），
     * 控件里的视角参数就跟着丢了 —— 所以视角状态得由外层（剪贴板界面）存着，
     * 重建完再塞回去，不然「转了轴回到上一页发现什么都没变」。
     */
    public PreviewCameraModel.ViewState viewState() {
        return cameraModel.viewState();
    }

    /** 恢复视角状态。 */
    public void setViewState(PreviewCameraModel.ViewState state) {
        cameraModel.setViewState(state);
    }

    /**
     * 绕世界某根轴把结构转 90°。
     *
     * @param axis  0=X，1=Y，2=Z
     * @param steps 转多少个 90°（正数按右手定则）
     */
    public void rotateAxis(int axis, int steps) {
        cameraModel.rotateAxis(axis, steps);
    }

    /** 回到默认视角：偏航 45°、俯仰 30°、缩放 100%、不平移、没有朝向修正。 */
    public void resetView() {
        cameraModel.reset();
    }

    /**
     * 把当前预览导出成图片（透明背景的 PNG）。
     * <p>
     * 存到游戏目录的 {@code mcoo_build_with_ease_exports/} 里，文件名带时间和分辨率。
     *
     * @param width  图片宽（像素）
     * @param height 图片高（像素）
     */
    public void exportImage(int width, int height) {
        if (this.renderer == null || this.blocks == null || this.blocks.isEmpty()) {
            return;
        }
        var directory = dev.huskuraft.universal.api.platform.Platform.getInstance()
                .getGameDir().resolve("mcoo_build_with_ease_exports");
        var name = "preview_" + java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                + "_" + width + "x" + height + ".png";
        this.renderer.exportModelPreview(
                new dev.huskuraft.universal.api.renderer.PreviewRenderRequest(
                        this.blocks, cameraModel.camera(), cameraModel.orientation()),
                width, height, directory.resolve(name));
        getEntrance().getClient().getPlayer().sendMessage(com.mcoo.buildwithease.McooBuildwithEase.getSystemMessage(
                dev.huskuraft.universal.api.text.Text.translate(
                        "mcoo_build_with_ease.clipboard.export_image.started", width + "×" + height)));
    }

    /**
     * 排队一次导出（在下一帧渲染时执行）。
     * <p>
     * 导出要用到渲染器，而渲染器是渲染的时候才拿到的 —— 所以不能当场导。
     *
     * @param width  图片宽（像素）
     * @param height 图片高（像素）
     */
    public void requestExport(int width, int height) {
        this.exportWidth = width;
        this.exportHeight = height;
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
            // 【方向】模型要跟着鼠标走 —— 往右拖，模型就往右转。
            // 之前这里是减号，方向正好相反（往右拖模型往左转）。
            cameraModel.rotateByDrag(deltaX, deltaY);
        } else if (button == 1) {
            // 右键：平移
            cameraModel.panByDrag(deltaX, deltaY);
        }
        return true;
    }

    @Override
    public boolean onMouseScrolled(double mouseX, double mouseY, double amountX, double amountY) {
        if (!isMouseOver(mouseX, mouseY)) {
            return false;
        }
        // 滚轮向上（amountY > 0）放大，向下缩小 —— 与常见的 3D 视图操作习惯一致。
        cameraModel.zoomByScroll(amountY);
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
