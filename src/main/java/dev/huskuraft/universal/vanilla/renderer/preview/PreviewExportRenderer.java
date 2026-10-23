package dev.huskuraft.universal.vanilla.renderer.preview;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import dev.huskuraft.universal.api.renderer.PreviewBlock;
import dev.huskuraft.universal.api.renderer.PreviewCamera;
import dev.huskuraft.universal.api.renderer.PreviewOrientation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;

import java.nio.file.Path;
import java.util.List;

/**
 * 「导出图片」用的离屏渲染器。
 * <p>
 * 画面用 {@link PreviewScene} 画（和预览完全一致），但**画到自己的纹理上** ——
 * 画中画通道自己那张离屏纹理没有「可回读」标志（usage 里没有 COPY_SRC），像素拷不出来。
 * <p>
 * <b>三个必须记住的坑（26.2）：</b>
 * <ol>
 *     <li>3D 内容必须在 GUI 渲染通道<b>里面</b>画（特性渲染器只在那个时候工作），
 *         所以这里仍然借用画中画通道的时机，只是把输出纹理换成自己那张可回读的。</li>
 *     <li>{@code CommandEncoder#copyTextureToBuffer} 的第 5 个参数是 <b>mip 层号</b>，
 *         <b>不是</b>「每行多少字节」。传错的话拷的是一个不存在的 mip 层，拷出来全是 0。</li>
 *     <li>{@code GpuDevice#createCommandEncoder()} 返回的是<b>共享的帧编码器</b>，
 *         自己调 {@code submit()} 会重置命令池，把这一帧的渲染命令一起干掉 →
 *         {@code VK_ERROR_DEVICE_LOST}。回读要靠 {@code copyTextureToBuffer} 的回调
 *         （原版截图就是这条路）。</li>
 * </ol>
 */
public class PreviewExportRenderer extends PictureInPictureRenderer<PreviewExportRenderState> {

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("McooBuildwithEase/PreviewExport");

    /** 当前实例（由 GameRendererMixin 在创建时登记）。 */
    private static PreviewExportRenderer instance;

    public static void register(PreviewExportRenderer renderer) {
        instance = renderer;
    }

    public static PreviewExportRenderer get() {
        return instance;
    }

    private final Minecraft minecraft;
    private final PreviewScene scene;

    /** 自己的投影（画中画通道那个的深度范围写死 ±1000，高分辨率导出时不够用）。 */
    private final net.minecraft.client.renderer.Projection projection = new net.minecraft.client.renderer.Projection();
    private final net.minecraft.client.renderer.ProjectionMatrixBuffer projectionMatrixBuffer =
            new net.minecraft.client.renderer.ProjectionMatrixBuffer("preview export");
    /** 自己的提交节点存储（画中画通道那个是私有的）。 */
    private final net.minecraft.client.renderer.SubmitNodeStorage storage =
            new net.minecraft.client.renderer.SubmitNodeStorage();
    /** 自己的深度纹理（和颜色纹理一样大）。 */
    private GpuTexture depthTexture;
    private GpuTextureView depthView;

    /** 本次导出用的离屏纹理（自己建的，带 COPY_SRC 才拷得出来）。 */
    private ExportTarget target;
    /** 排队等着画进纹理的内容。 */
    private Pending pending;
    /** 这一帧的状态已经提交过了（防止一帧里提交多次导致重复渲染）。 */
    private boolean stateSubmitted;
    /** 已经画进纹理了，等回读。 */
    private boolean rendered;
    /** 回读命令已经排上队了。 */
    private boolean readBackScheduled;
    /** 这次导出要存到哪。 */
    private Path targetFile;
    /** 导出图片的最终尺寸（请求的尺寸，回读后居中裁切到这个大小）。 */
    private int outWidth;
    private int outHeight;

    public PreviewExportRenderer(Minecraft minecraft) {
        this.minecraft = minecraft;
        this.scene = new PreviewScene(minecraft);
    }

    @Override
    public Class<PreviewExportRenderState> getRenderStateClass() {
        return PreviewExportRenderState.class;
    }

    @Override
    protected String getTextureLabel() {
        return "structure export";
    }

    /**
     * 离屏纹理里的 Y 方向平移量 —— 返回一半，内容在视口里居中。
     * <p>
     * 离屏纹理的 Y 轴**向下**（orthoInvertY），原点在左上角。
     */
    @Override
    protected float getTranslateY(int height, int guiScale) {
        return height / 2.0F;
    }

    /**
     * 导出用的图<b>不贴回界面</b> —— 它只用来回读成文件。
     * <p>
     * 不贴的话玩家会看到一大张图突然糊在屏幕上。
     */
    @Override
    protected void blitTexture(PreviewExportRenderState state, GuiRenderState guiRenderState) {
        // 故意不贴回界面
    }

    /**
     * 这个通道永远要重画（导出图是一次性的，画完就要回读，不能走「纹理已就绪就复用」的短路）。
     */
    @Override
    protected boolean textureIsReadyToBlit(PreviewExportRenderState state) {
        return false;
    }

    /**
     * 自己准备离屏渲染环境。
     * <p>
     * <b>为什么不直接用基类的 {@code prepare}</b>：基类把正交视锥的深度范围写死成 ±1000，
     * 而模型在视图空间里的深度是随分辨率线性变大的 ——
     * 4K 导出时结构在视图 Z 上能占三千多，<b>会被近裁剪面削掉一块</b>（正对相机的那个角先没了）。
     * 所以这里自己搭投影：深度范围按模型实际厚度算。
     */
    @Override
    public void prepare(PreviewExportRenderState state, GuiRenderState guiRenderState,
                         net.minecraft.client.renderer.feature.FeatureRenderDispatcher dispatcher, int guiScale) {
        var target = this.target;
        if (target == null) {
            return;
        }
        var width = (state.x1() - state.x0()) * guiScale;
        var height = (state.y1() - state.y0()) * guiScale;

        // 深度纹理（必须和颜色纹理一样大）
        if (this.depthTexture == null
                || this.depthTexture.getWidth(0) != width
                || this.depthTexture.getHeight(0) != height) {
            if (this.depthView != null) {
                this.depthView.close();
                this.depthView = null;
            }
            if (this.depthTexture != null) {
                this.depthTexture.close();
                this.depthTexture = null;
            }
            var device = RenderSystem.getDevice();
            this.depthTexture = device.createTexture(() -> "preview export depth",
                    GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_RENDER_ATTACHMENT,
                    com.mojang.blaze3d.GpuFormat.D32_FLOAT, width, height, 1, 1);
            this.depthView = device.createTextureView(this.depthTexture);
        }

        // 投影：深度范围按模型在视图 Z 上的厚度算（留 50% 余量）
        var poseScale = guiScale * state.scale();
        var modelDepth = poseScale
                * PreviewScale.depthExtent(state.blocks(), state.camera(), state.orientation());
        var depthRange = Math.max(1000.0F, modelDepth * 1.5F);
        this.projection.setupOrtho(-depthRange, depthRange, width, height, true);
        RenderSystem.setProjectionMatrix(this.projectionMatrixBuffer.getBuffer(this.projection),
                com.mojang.blaze3d.ProjectionType.ORTHOGRAPHIC);

        RenderSystem.outputColorTextureOverride = target.view();
        RenderSystem.outputDepthTextureOverride = this.depthView;

        var modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushMatrix();
        var poseStack = new PoseStack();
        poseStack.translate(width / 2.0F, getTranslateY(height, guiScale), 0.0F);
        var scale = guiScale * state.scale();
        // Z 取反是画中画通道的老规矩（正交投影把 +Y 映到了图像下方，等效于相机站在模型背后）
        poseStack.scale(scale, scale, -scale);

        renderToTexture(state, poseStack, this.storage);

        dispatcher.renderAllFeatures(this.storage);

        modelViewStack.popMatrix();
        RenderSystem.outputColorTextureOverride = null;
        RenderSystem.outputDepthTextureOverride = null;
        // 贴回界面这一步故意不做 —— 导出图只用来存文件，贴出来会糊玩家一脸
    }

    /**
     * 登记一次导出请求。
     *
     * @param blocks      要画的内容
     * @param camera      相机
     * @param orientation 朝向修正
     * @param width       图片宽（物理像素）
     * @param height      图片高（物理像素）
     * @param file        存到哪（PNG）
     */
    public void request(List<PreviewBlock> blocks, PreviewCamera camera, PreviewOrientation orientation,
                        int width, int height, Path file) {
        if (blocks == null || blocks.isEmpty()) {
            return;
        }
        var guiScale = Math.max(1, minecraft.gameRenderer.gameRenderState().windowRenderState.guiScale);
        // 画中画通道的离屏纹理尺寸 = 逻辑尺寸 × guiScale，我们自己的纹理必须**一模一样**
        // （渲染通道的视口是按通道那张纹理算的，尺寸对不上画面就错位）。
        // 逻辑尺寸只能取整，所以物理尺寸和请求的尺寸可能差几个像素 ——
        // 回读之后居中裁切到请求的尺寸，导出的 PNG 就是**正好**请求的那个分辨率。
        var logicalWidth = Math.max(1, (int) Math.ceil(width / (float) guiScale));
        var logicalHeight = Math.max(1, (int) Math.ceil(height / (float) guiScale));
        var physicalWidth = logicalWidth * guiScale;
        var physicalHeight = logicalHeight * guiScale;

        // 上一次已经排上回读的话，它的纹理由回调自己关（这时候关会把还没拷完的纹理干掉）
        if (!this.readBackScheduled) {
            closeTarget();
        }

        var device = RenderSystem.getDevice();
        var texture = device.createTexture(() -> "preview export",
                GpuTexture.USAGE_COPY_DST
                        | GpuTexture.USAGE_COPY_SRC
                        | GpuTexture.USAGE_TEXTURE_BINDING
                        | GpuTexture.USAGE_RENDER_ATTACHMENT,
                com.mojang.blaze3d.GpuFormat.RGBA8_UNORM, physicalWidth, physicalHeight, 1, 1);
        this.target = new ExportTarget(texture, device.createTextureView(texture));
        this.pending = new Pending(blocks, camera, orientation, logicalWidth, logicalHeight, width, height);
        this.targetFile = file;
        this.stateSubmitted = false;
        this.rendered = false;
        this.readBackScheduled = false;
        if (Boolean.getBoolean("mcoo.debugPreview")) {
            LOGGER.info("[export] 排队 {}x{} -> 纹理 {}x{} (guiScale={})",
                    logicalWidth, logicalHeight, physicalWidth, physicalHeight, guiScale);
        }
    }

    /**
     * 提交导出用的状态（每帧调用一次）。
     * <p>
     * 状态要提交到画中画通道里（3D 内容得在 GUI 渲染通道内部画），
     * 所以这里摆在屏幕外 —— 通道会认为它被某个 GUI 元素盖住、直接丢掉。
     */
    public void submit(GuiRenderState guiRenderState) {
        var pending = this.pending;
        if (pending == null || this.stateSubmitted) {
            return;
        }
        this.stateSubmitted = true;
        var x0 = -8192;
        var y0 = -8192;
        var scissor = new ScreenRectangle(x0, y0, pending.logicalWidth(), pending.logicalHeight());
        guiRenderState.addPicturesInPictureState(new PreviewExportRenderState(
                pending.blocks(), pending.camera(), pending.orientation(),
                x0, y0, x0 + pending.logicalWidth(), y0 + pending.logicalHeight(),
                scissor,
                PictureInPictureRenderState.getBounds(
                        x0, y0, x0 + pending.logicalWidth(), y0 + pending.logicalHeight(), scissor)));
    }

    @Override
    protected void renderToTexture(PreviewExportRenderState state, PoseStack poseStack, SubmitNodeCollector collector) {
        var pending = this.pending;
        this.pending = null;
        this.stateSubmitted = false;
        var target = this.target;
        if (pending == null || pending.blocks().isEmpty() || target == null) {
            return;
        }
        if (Boolean.getBoolean("mcoo.debugPreview")) {
            LOGGER.info("[export] renderToTexture 被调用，方块数={}", pending.blocks().size());
        }
        // 背景清成全透明（PNG 要透明背景），深度也一起清掉（深度纹理是我们自己的）。
        // 用共享编码器记一条清屏命令 —— 它排在后面那个渲染通道之前，顺序是对的。
        RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(
                target.texture(), new org.joml.Vector4f(0.0F, 0.0F, 0.0F, 0.0F),
                this.depthTexture, 0.0D);
        // 【输出不要还原】真正的绘制发生在 renderToTexture **之后**
        // （prepare 里紧接着就是 renderAllFeatures），这里还原了就等于画回通道自己的纹理去了。
        scene.render(pending.blocks(), pending.camera(), pending.orientation(),
                Math.max(0.0001F, state.scale()), poseStack, collector);
        this.rendered = true;
        this.outWidth = pending.outWidth();
        this.outHeight = pending.outHeight();
        if (Boolean.getBoolean("mcoo.debugPreview")) {
            LOGGER.info("[export] 离屏渲染完成，纹理={}x{}",
                    target.texture().getWidth(0), target.texture().getHeight(0));
        }
    }

    /**
     * 把上一帧画好的图回读存盘。
     * <p>
     * <b>必须等上一帧真的画完</b>：回读是「纹理 → GPU 缓冲 → 映射回 CPU」，
     * 排在渲染命令后面才有正确的像素。所以这里挂在客户端 tick 上（帧与帧之间），
     * 而不是渲染过程里。
     */
    public void readBackIfReady() {
        var target = this.target;
        if (!this.rendered || this.readBackScheduled || target == null) {
            return;
        }
        this.readBackScheduled = true;
        var texture = target.texture();
        var file = this.targetFile;
        var width = texture.getWidth(0);
        var height = texture.getHeight(0);
        var size = (long) width * height * 4L;
        var outW = this.outWidth;
        var outH = this.outHeight;
        var device = RenderSystem.getDevice();
        var buffer = device.createBuffer(() -> "preview export readback",
                GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_MAP_READ, size);
        if (Boolean.getBoolean("mcoo.debugPreview")) {
            LOGGER.info("[export] 排队回读 {}x{} -> {}", width, height, file);
        }
        // 【第 5 个参数是 mip 层号，不是每行字节数】传 0 = 整张图一起拷。
        // 【不要自己 submit()】createCommandEncoder() 返回的是共享的帧编码器，
        // 自己 submit 会重置命令池 → VK_ERROR_DEVICE_LOST。
        // 回读放在回调里（原版截图就是这么干的）：回调在 GPU 拷完之后才跑。
        device.createCommandEncoder().copyTextureToBuffer(texture, buffer, 0L, () -> {
            try {
                writePng(buffer, width, height, size, file, outW, outH);
            } catch (Throwable throwable) {
                LOGGER.error("导出图片失败", throwable);
            } finally {
                buffer.close();
                if (this.target == target) {
                    closeTarget();
                } else {
                    target.close();
                }
            }
        }, 0);
    }

    /**
     * 把回读到的像素写成 PNG（透明背景）。
     */
    private void writePng(GpuBuffer buffer, int width, int height, long size, Path file, int outWidth, int outHeight) {
        byte[] raw;
        var mapped = buffer.map(0, size, true, false);
        try {
            raw = new byte[(int) size];
            mapped.data().get(0, raw);
        } finally {
            mapped.close();
        }
        var nonZero = countNonZero(raw);
        if (Boolean.getBoolean("mcoo.debugPreview")) {
            LOGGER.info("[export] 回读 {}x{} 非零像素={}", width, height, nonZero);
        }
        // PNG 编码挺慢（4K 要一秒多），扔到后台线程，别卡住渲染
        var texWidth = width;
        var texHeight = height;
        var pngWidth = outWidth;
        var pngHeight = outHeight;
        var thread = new Thread(() -> {
            try {
                writePngFile(raw, texWidth, texHeight, pngWidth, pngHeight, file);
                LOGGER.info("导出图片到 {}", file);
            } catch (Throwable throwable) {
                LOGGER.error("导出图片失败", throwable);
            }
        }, "mcoo-export-png");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * 统计非零像素（诊断用：全 0 就说明回读还是空的）。
     */
    private static int countNonZero(byte[] raw) {
        var count = 0;
        for (int i = 0; i + 3 < raw.length; i += 4) {
            if ((raw[i] & 0xFF) != 0 || (raw[i + 1] & 0xFF) != 0
                    || (raw[i + 2] & 0xFF) != 0 || (raw[i + 3] & 0xFF) != 0) {
                count++;
            }
        }
        return count;
    }

    /**
     * 把 RGBA8 的原始字节写成 PNG。
     * <p>
     * 纹理里的第 0 行是图像的**下面**一行（帧缓冲原点在左下），PNG 的第 0 行是最上面 → 倒着写。
     * Alpha 原样保留 —— 背景是清成全透明的，这就是「透明背景 PNG」。
     * <p>
     * 离屏纹理的物理尺寸是「逻辑尺寸 × guiScale」，和请求的尺寸可能差几个像素 ——
     * 这里居中裁切到<b>正好</b>请求的尺寸。
     */
    private static void writePngFile(byte[] raw, int width, int height, int outWidth, int outHeight, Path file)
            throws Exception {
        var cropX = Math.max(0, (width - outWidth) / 2);
        var cropY = Math.max(0, (height - outHeight) / 2);
        var pixels = new int[outWidth * outHeight];
        for (int y = 0; y < outHeight; y++) {
            // 纹理第 0 行是图像的最下面一行，倒着取；同时扣掉上面裁掉的 cropY 行
            var src = (height - 1 - cropY - y) * width * 4;
            var dst = y * outWidth;
            for (int x = 0; x < outWidth; x++) {
                var o = src + (cropX + x) * 4;
                var r = raw[o] & 0xFF;
                var g = raw[o + 1] & 0xFF;
                var b = raw[o + 2] & 0xFF;
                var a = raw[o + 3] & 0xFF;
                pixels[dst + x] = (a << 24) | (r << 16) | (g << 8) | b;
            }
        }
        var image = new java.awt.image.BufferedImage(outWidth, outHeight,
                java.awt.image.BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, outWidth, outHeight, pixels, 0, outWidth);
        var parent = file.getParent();
        if (parent != null) {
            java.nio.file.Files.createDirectories(parent);
        }
        javax.imageio.ImageIO.write(image, "png", file.toFile());
    }

    /** 关掉当前这张导出纹理。 */
    private void closeTarget() {
        if (this.target != null) {
            this.target.close();
            this.target = null;
        }
    }

    @Override
    public void close() {
        this.pending = null;
        this.stateSubmitted = false;
        this.rendered = false;
        this.readBackScheduled = false;
        this.targetFile = null;
        closeTarget();
        if (this.depthView != null) {
            this.depthView.close();
            this.depthView = null;
        }
        if (this.depthTexture != null) {
            this.depthTexture.close();
            this.depthTexture = null;
        }
        this.projectionMatrixBuffer.close();
        super.close();
    }

    /** 一次导出用的纹理（颜色纹理 + 它的视图）。 */
    private record ExportTarget(GpuTexture texture, GpuTextureView view) implements AutoCloseable {
        @Override
        public void close() {
            view.close();
            texture.close();
        }
    }

    /** 排队等着导出的一次请求。 */
    private record Pending(
            List<PreviewBlock> blocks,
            PreviewCamera camera,
            PreviewOrientation orientation,
            int logicalWidth,
            int logicalHeight,
            int outWidth,
            int outHeight
    ) {
    }
}
