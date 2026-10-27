package com.mcoo.buildwithease.screen.imgui.panel;

import com.mcoo.buildwithease.McooBuildwithEaseClient;
import com.mcoo.buildwithease.screen.clipboard.PreviewCameraModel;
import com.mcoo.buildwithease.screen.clipboard.PreviewSource;
import com.mcoo.buildwithease.screen.imgui.widget.ImGuiShell;
import com.mcoo.buildwithease.screen.imgui.widget.ImGuiWidgets;
import dev.huskuraft.universal.api.renderer.PreviewBlock;
import dev.huskuraft.universal.api.renderer.PreviewRenderRequest;
import dev.huskuraft.universal.api.text.Text;
import dev.huskuraft.universal.vanilla.gui.McooImGuiTextures;

import java.util.List;

/**
 * 剪贴板面板：中间栏是结构的 <b>3D 预览</b>（拖动旋转 / 滚轮缩放 / 右键平移），右栏是视角与导出操作。
 *
 * <h3>3D 预览怎么进 ImGui</h3>
 * ImGui 只会画 2D，而结构预览必须走 MC 的 3D 管线（否则箱子、钟、旗帜这些方块实体画不出来）。
 * 所以：
 * <ol>
 *     <li>面板通过 {@link #nativePreview()} 告诉平台「要预览哪些方块」；</li>
 *     <li>平台把它作为画中画（PiP）状态提交，原版把结构渲染进离屏纹理；</li>
 *     <li>面板用 {@code McooImGuiTextures.previewTextureId()} 拿到纹理 id，在中间栏画出来；</li>
 *     <li>鼠标拖动由 ImGui 提供增量，喂给 {@link PreviewCameraModel}（和自研 GUI 用同一份模型）。</li>
 * </ol>
 *
 * ⚠️ 纹理内容可能比输入晚一帧（平台渲染与 ImGui 绘制的先后不保证）。60fps 下基本看不出来。
 */
public final class ClipboardPanel implements ImGuiPanel {

    private static final String ID = "clipboard";

    private final McooBuildwithEaseClient entrance;
    /** 视角状态（与自研 GUI 的预览控件共用同一份模型）。 */
    private final PreviewCameraModel camera = new PreviewCameraModel();

    /** 上次构建时的结构指纹 + 方块列表（结构没变就不重建）。 */
    private List<PreviewBlock> blocks;
    private int blocksFingerprint;

    public ClipboardPanel(McooBuildwithEaseClient entrance) {
        this.entrance = entrance;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String titleKey() {
        return "mcoo_build_with_ease.clipboard.edit.title";
    }

    // ------------------------------------------------------------------
    // 中间栏：3D 预览
    // ------------------------------------------------------------------

    @Override
    public void renderContent() {
        var blocks = blocks();
        if (blocks.isEmpty()) {
            ImGuiShell.contentPlaceholder("mcoo_build_with_ease.clipboard.empty");
            return;
        }

        long textureId = McooImGuiTextures.previewTextureId();
        if (textureId == 0L) {
            // 第一帧：平台还没渲染过预览，给一句提示而不是空着
            ImGuiWidgets.text("mcoo_build_with_ease.clipboard.rendering");
            return;
        }

        var avail = imgui.ImGui.getContentRegionAvail();
        float width = Math.max(1f, avail.x);
        float height = Math.max(1f, avail.y);

        // 离屏纹理已经是「左上角原点、Y 向下」（见 PreviewPictureInPictureRenderer 的说明），
        // 所以 UV 不需要翻转。
        ImGuiWidgets.image(textureId, width, height, new float[]{0f, 0f, 1f, 1f});

        if (imgui.ImGui.isItemHovered()) {
            var io = imgui.ImGui.getIO();
            float deltaX = io.getMouseDeltaX();
            float deltaY = io.getMouseDeltaY();
            if (imgui.ImGui.isMouseDown(0) && (deltaX != 0f || deltaY != 0f)) {
                camera.rotateByDrag(deltaX, deltaY);
            } else if (imgui.ImGui.isMouseDown(1) && (deltaX != 0f || deltaY != 0f)) {
                camera.panByDrag(deltaX, deltaY);
            }
            float wheel = io.getMouseWheel();
            if (wheel != 0f) {
                camera.zoomByScroll(wheel);
            }
        }
    }

    // ------------------------------------------------------------------
    // 右栏：视角操作 + 导出
    // ------------------------------------------------------------------

    @Override
    public void renderDetails() {
        ImGuiShell.panelTitle("mcoo_build_with_ease.shell.details");

        if (ImGuiWidgets.button("mcoo_build_with_ease.clipboard.viewpoint.title")) {
            camera.reset();
        }
        if (ImGuiWidgets.button("mcoo_build_with_ease.clipboard.rotate_y")) {
            camera.rotateAxis(1, 1);
        }
        if (ImGuiWidgets.button("mcoo_build_with_ease.clipboard.export_png")) {
            exportImage();
        }

        ImGuiWidgets.separator();
        ImGuiWidgets.text("mcoo_build_with_ease.clipboard.hint");
    }

    /**
     * 导出透明背景 PNG。
     * <p>
     * ⚠️ 这里直接调了平台层的导出渲染器 —— 业务层碰 MC 适配层是<b>受控的例外</b>：
     * 和 {@code McooImGuiTextures} 一样，ImGui 面板本来就只可能跑在客户端，
     * 而 {@code Renderer} 实例只在界面绘制的回调里才有（api 的 {@code Client} 不暴露它），
     * 面板拿不到。真要收口的话，应该给 api 加一个「导出请求」入口，而不是在这里拼。
     */
    private void exportImage() {
        var blocks = blocks();
        if (blocks.isEmpty()) {
            return;
        }
        var renderer = dev.huskuraft.universal.vanilla.renderer.preview.PreviewExportRenderer.get();
        if (renderer == null) {
            return;
        }
        var directory = dev.huskuraft.universal.api.platform.Platform.getInstance()
                .getGameDir().resolve("mcoo_build_with_ease_exports");
        var name = "preview_" + java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".png";
        renderer.request(blocks, camera.camera(), camera.orientation(),
                1024, 1024, directory.resolve(name));
        entrance.getClient().getPlayer().sendMessage(Text.translate(
                "mcoo_build_with_ease.clipboard.export_image.started", "1024×1024"));
    }

    // ------------------------------------------------------------------
    // 原生预览（平台层用）
    // ------------------------------------------------------------------

    @Override
    public PreviewRenderRequest nativePreview() {
        var blocks = blocks();
        if (blocks.isEmpty()) {
            return null;
        }
        var bounds = nativePreviewBounds();
        if (bounds == null || bounds[2] <= 0 || bounds[3] <= 0) {
            return null;
        }
        return request();
    }

    @Override
    public int[] nativePreviewBounds() {
        // 中间栏整个矩形（ImGuiShell 在布局时记下的绝对屏幕坐标）
        return ImGuiShell.centerRect();
    }

    private PreviewRenderRequest request() {
        return new PreviewRenderRequest(blocks(), camera.camera(), camera.orientation());
    }

    /** 当前要预览的方块（结构没变就复用上次的结果）。 */
    private List<PreviewBlock> blocks() {
        var snapshot = snapshot();
        if (snapshot == null) {
            return List.of();
        }
        var fingerprint = PreviewSource.fingerprintOf(snapshot);
        if (blocks == null || blocksFingerprint != fingerprint) {
            blocks = PreviewSource.blocksOf(snapshot);
            blocksFingerprint = fingerprint;
        }
        return blocks;
    }

    private com.mcoo.buildwithease.building.clipboard.Snapshot snapshot() {
        var player = entrance.getClient().getPlayer();
        if (player == null) {
            return null;
        }
        var clipboard = entrance.getStructureBuilder().getContext(player).clipboard();
        return clipboard == null ? null : clipboard.snapshot();
    }

}
