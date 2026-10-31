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

    /** 导出尺寸选项（像素）：1K / 2K / 4K。 */
    private static final int[] EXPORT_SIZES = {1024, 2048, 4096};
    private int exportSizeIndex = 0;

    /** 视角状态（与自研 GUI 的预览控件共用同一份模型）。 */
    private final PreviewCameraModel camera = new PreviewCameraModel();

    /** 上次构建时的结构指纹 + 方块列表（结构没变就不重建）。 */
    private List<PreviewBlock> blocks;
    private int blocksFingerprint;
    /** 最近一次结构导出的结果（路径或错误），显示在右栏。 */
    private String exportStatus;
    /** 结构文件导出：正在选目录（中间栏切成目录浏览器）。 */
    private boolean exporting;
    private dev.huskuraft.universal.api.export.StructureExporter.Format exportFormat;
    private java.nio.file.Path exportDirectory;
    private final imgui.type.ImString exportName = new imgui.type.ImString("structure", 64);
    private final imgui.type.ImString newFolderName = new imgui.type.ImString("", 32);

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
        if (exporting) {
            renderExportBrowser();
            return;
        }
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

        // 历史先画：它要占底部一块固定空间，预览吃剩下的高度。
        // 反过来（先画满屏预览、再画历史）历史会被挤出可视区 —— 踩过。
        renderHistory();

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

        // 剪贴板总开关：关掉时粘贴/快捷粘贴整体失效（和自研界面同一份状态）
        var clipboard = clipboard();
        if (clipboard != null) {
            boolean clipboardEnabled = clipboard.enabled();
            if (ImGuiShell.fieldCheckbox("mcoo_build_with_ease.clipboard.enable", clipboardEnabled)) {
                var player = entrance.getClient().getPlayer();
                if (player != null) {
                    entrance.getStructureBuilder().setClipboard(player, clipboard.withEnabled(!clipboardEnabled));
                }
            }
            ImGuiWidgets.separator();
        }

        // 清空剪贴板（旧 ClipboardScreen 的 Clear 按钮）
        if (clipboard != null && !clipboard.isEmpty()) {
            if (ImGuiWidgets.button("mcoo_build_with_ease.button.clear")) {
                var player = entrance.getClient().getPlayer();
                if (player != null) {
                    entrance.getStructureBuilder().setClipboard(player,
                            clipboard.withSnapshot(com.mcoo.buildwithease.building.clipboard.Snapshot.EMPTY));
                    blocks = null;
                    blocksFingerprint = 0;
                }
            }
            ImGuiWidgets.separator();
        }
        // 滑条：拖动旋转不可能精确到 1°，微调一律走这里。
        // 也顺带兜住「鼠标拖动在某些机器上不跟手」的情况 —— 滑条是 ImGui 的标准控件，
        // 走的是和按钮同一套输入路径。
        float yaw = ImGuiShell.fieldSliderFloat("mcoo_build_with_ease.clipboard.slider_yaw",
                camera.yaw(), -180f, 180f);
        if (yaw != camera.yaw()) {
            camera.setYaw(yaw);
        }
        float pitch = ImGuiShell.fieldSliderFloat("mcoo_build_with_ease.clipboard.slider_pitch",
                camera.pitch(), -90f, 90f);
        if (pitch != camera.pitch()) {
            camera.setPitch(pitch);
        }
        float zoom = ImGuiShell.fieldSliderFloat("mcoo_build_with_ease.clipboard.slider_zoom",
                camera.zoom(), 0.2f, 8f);
        if (zoom != camera.zoom()) {
            camera.setZoom(zoom);
        }

        ImGuiWidgets.separator();

        if (ImGuiWidgets.button("mcoo_build_with_ease.clipboard.viewpoint.title")) {
            camera.reset();
        }
        if (ImGuiWidgets.button("mcoo_build_with_ease.clipboard.rotate_y")) {
            camera.rotateAxis(1, 1);
        }
        exportSizeIndex = ImGuiShell.fieldCombo("mcoo_build_with_ease.clipboard.export_size",
                new String[]{"1024", "2048", "4096"}, exportSizeIndex);
        if (ImGuiWidgets.button("mcoo_build_with_ease.clipboard.export_png")) {
            exportImage();
        }

        ImGuiWidgets.separator();
        ImGuiWidgets.text("mcoo_build_with_ease.clipboard.download.title");
        if (ImGuiWidgets.button("mcoo_build_with_ease.clipboard.download.litematic")) {
            beginExport(dev.huskuraft.universal.api.export.StructureExporter.Format.LITEMATIC);
        }
        if (ImGuiWidgets.button("mcoo_build_with_ease.clipboard.download.worldedit")) {
            beginExport(dev.huskuraft.universal.api.export.StructureExporter.Format.WORLDEDIT);
        }
        if (ImGuiWidgets.button("mcoo_build_with_ease.clipboard.download.axiom")) {
            beginExport(dev.huskuraft.universal.api.export.StructureExporter.Format.AXIOM);
        }
        if (exportStatus != null) {
            ImGuiWidgets.diagnostic(exportStatus);
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
        int size = EXPORT_SIZES[Math.max(0, Math.min(exportSizeIndex, EXPORT_SIZES.length - 1))];
        renderer.request(blocks, camera.camera(), camera.orientation(),
                size, size, directory.resolve(name));
        entrance.getClient().getPlayer().sendMessage(Text.translate(
                "mcoo_build_with_ease.clipboard.export_image.started", size + "×" + size));
    }

    /** 一次导出所需的数据：方块（已转相对坐标）+ 结构尺寸。 */
    private record ExportPayload(List<dev.huskuraft.universal.api.export.StructureExporter.Block> blocks,
                                 int sizeX, int sizeY, int sizeZ) {
    }

    /** 把剪贴板转成导出数据；空剪贴板返回 null。 */
    private ExportPayload payload() {
        var snapshot = snapshot();
        if (snapshot == null || snapshot.isEmpty()) {
            return null;
        }
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        var raw = new java.util.ArrayList<com.mcoo.buildwithease.building.clipboard.BlockData>();
        for (var blockData : snapshot.blockData()) {
            if (blockData.blockState() == null) {
                continue;
            }
            raw.add(blockData);
            var position = blockData.blockPosition();
            minX = Math.min(minX, position.x());
            minY = Math.min(minY, position.y());
            minZ = Math.min(minZ, position.z());
            maxX = Math.max(maxX, position.x());
            maxY = Math.max(maxY, position.y());
            maxZ = Math.max(maxZ, position.z());
        }
        if (raw.isEmpty()) {
            return null;
        }
        // 导出器要的是「相对结构左下角」的坐标
        final int baseX = minX, baseY = minY, baseZ = minZ;
        var blocks = raw.stream()
                .map(blockData -> {
                    var position = blockData.blockPosition();
                    return new dev.huskuraft.universal.api.export.StructureExporter.Block(
                            position.x() - baseX, position.y() - baseY, position.z() - baseZ,
                            blockData.blockState());
                })
                .toList();
        return new ExportPayload(blocks, maxX - minX + 1, maxY - minY + 1, maxZ - minZ + 1);
    }

    /**
     * 选好格式后进「游戏内目录浏览器」，起点是该格式的默认目录。
     * <p>
     * ⚠️ 导出<b>不能直接落盘</b>（`AGENTS.md` §10.3）：玩家要能把文件存到任意位置分享给别人。
     * 旧流程是独立的 {@code SaveFileScreen}，这里把它收进面板中间栏，只列子目录 + 文件名输入框。
     */
    private void beginExport(dev.huskuraft.universal.api.export.StructureExporter.Format format) {
        if (payload() == null) {
            exportStatus = Text.translate("mcoo_build_with_ease.clipboard.empty").getString();
            return;
        }
        try {
            exportFormat = format;
            exportDirectory = dev.huskuraft.universal.api.platform.PlatformLoader
                    .getSingleton(dev.huskuraft.universal.api.export.StructureExporter.class)
                    .defaultDirectory(format);
            exportName.set("structure_" + java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
            exportStatus = null;
            exporting = true;
        } catch (Throwable error) {
            exportStatus = error.getClass().getSimpleName();
        }
    }

    private void renderExportBrowser() {
        ImGuiWidgets.text("mcoo_build_with_ease.clipboard.download.title");
        ImGuiWidgets.diagnostic(exportDirectory == null ? "?" : exportDirectory.toString());
        ImGuiWidgets.separator();

        if (exportDirectory != null) {
            var parent = exportDirectory.getParent();
            if (parent != null) {
                if (ImGuiShell.navItemRaw(Text.translate("mcoo_build_with_ease.button.parent_folder").getString(), false)) {
                    exportDirectory = parent;
                }
            }
            try (var stream = java.nio.file.Files.list(exportDirectory)) {
                var directories = stream
                        .filter(java.nio.file.Files::isDirectory)
                        .sorted(java.util.Comparator.comparing(path -> path.getFileName().toString().toLowerCase(java.util.Locale.ROOT)))
                        .limit(50)
                        .toList();
                for (var directory : directories) {
                    if (ImGuiShell.navItemRaw(directory.getFileName().toString(), false)) {
                        exportDirectory = directory;
                    }
                }
            } catch (Throwable error) {
                ImGuiWidgets.diagnostic(error.getClass().getSimpleName());
            }

            ImGuiWidgets.separator();
            imgui.ImGui.setNextItemWidth(160f);
            imgui.ImGui.inputText("##new_folder", newFolderName);
            imgui.ImGui.sameLine();
            if (ImGuiWidgets.button("mcoo_build_with_ease.button.new_folder")) {
                String folder = newFolderName.get() == null ? "" : newFolderName.get().trim();
                if (!folder.isEmpty()) {
                    try {
                        var created = exportDirectory.resolve(folder);
                        java.nio.file.Files.createDirectories(created);
                        exportDirectory = created;
                        newFolderName.set("");
                    } catch (Throwable error) {
                        exportStatus = error.getClass().getSimpleName();
                    }
                }
            }
            if (ImGuiWidgets.button("mcoo_build_with_ease.clipboard.download.to_downloads")) {
                var downloads = downloadsDirectory();
                if (downloads != null) {
                    exportDirectory = downloads;
                }
            }
        }

        ImGuiWidgets.separator();
        ImGuiWidgets.text("mcoo_build_with_ease.clipboard.download.file_name");
        imgui.ImGui.setNextItemWidth(-1f);
        imgui.ImGui.inputText("##export_name", exportName);
        if (ImGuiWidgets.button("mcoo_build_with_ease.button.save")) {
            saveExport();
        }
        imgui.ImGui.sameLine();
        if (ImGuiWidgets.button("mcoo_build_with_ease.button.cancel")) {
            exporting = false;
        }
        if (exportStatus != null) {
            ImGuiWidgets.diagnostic(exportStatus);
        }
    }

    private void saveExport() {        var payload = payload();
        if (payload == null || exportDirectory == null || exportFormat == null) {
            return;
        }
        String name = exportName.get() == null ? "" : exportName.get().trim();
        if (name.isEmpty()) {
            name = "structure";
        }
        try {
            var path = dev.huskuraft.universal.api.platform.PlatformLoader
                    .getSingleton(dev.huskuraft.universal.api.export.StructureExporter.class)
                    .export(exportDirectory, name, payload.sizeX(), payload.sizeY(), payload.sizeZ(),
                            payload.blocks(), exportFormat);
            exportStatus = path.toString();
            exporting = false;
        } catch (Throwable error) {
            exportStatus = error.getClass().getSimpleName()
                    + (error.getMessage() == null ? "" : ": " + error.getMessage());
        }
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
    /**
     * 快照历史（配置里持久化的最近若干份）。
     * <p>
     * 点一条就把它载入成当前剪贴板 —— 这是历史列表唯一有意义的动作；
     * 「删除 / 收藏」这类留给集合（collections）那一套，不在这一版里混。
     */
    private void renderHistory() {
        var history = entrance.getConfigStorage().get().clipboardConfig().history();
        if (history == null || history.isEmpty()) {
            return;
        }
        ImGuiWidgets.separator();
        ImGuiWidgets.text("mcoo_build_with_ease.clipboard.history");
        // 只列最近 8 条：历史可能很长，面板纵向空间有限
        int shown = Math.min(history.size(), 8);
        for (int i = history.size() - shown; i < history.size(); i++) {
            var entry = history.get(i);
            String label = "[" + (i + 1) + "] " + entry.blockData().size() + " 方块  "
                    + java.time.Instant.ofEpochMilli(entry.createdTimestamp());
            if (imgui.ImGui.selectable(label + "##history_" + i, false)) {
                loadHistory(entry);
            }
        }
    }

    /** 把一条历史快照载入当前剪贴板。 */
    private void loadHistory(com.mcoo.buildwithease.building.clipboard.Snapshot snapshot) {
        var player = entrance.getClient().getPlayer();
        if (player == null || snapshot == null) {
            return;
        }
        var clipboard = entrance.getStructureBuilder().getContext(player).clipboard();
        if (clipboard == null) {
            return;
        }
        entrance.getStructureBuilder().setClipboard(player, clipboard.withSnapshot(snapshot));
        // 载入后结构变了 → 强制下一次重建方块列表
        blocks = null;
        blocksFingerprint = 0;
    }
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

    /** 当前剪贴板（可能没有会话 → null）。 */
    private com.mcoo.buildwithease.building.clipboard.Clipboard clipboard() {
        var player = entrance.getClient().getPlayer();
        if (player == null) {
            return null;
        }
        return entrance.getStructureBuilder().getContext(player).clipboard();
    }

    private com.mcoo.buildwithease.building.clipboard.Snapshot snapshot() {
        var player = entrance.getClient().getPlayer();
        if (player == null) {
            return null;
        }
        var clipboard = entrance.getStructureBuilder().getContext(player).clipboard();
        return clipboard == null ? null : clipboard.snapshot();
    }

    /** 系统「下载」目录；找不到返回 null（Windows/macOS 一般是 Downloads，个别本地化环境叫「下载」）。 */
    private static java.nio.file.Path downloadsDirectory() {
        var home = java.nio.file.Path.of(System.getProperty("user.home", "."));
        for (var name : new String[]{"Downloads", "下载"}) {
            var candidate = home.resolve(name);
            if (java.nio.file.Files.isDirectory(candidate)) {
                return candidate;
            }
        }
        return null;
    }

}
