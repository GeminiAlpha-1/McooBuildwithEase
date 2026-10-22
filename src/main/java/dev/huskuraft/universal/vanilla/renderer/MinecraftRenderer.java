package dev.huskuraft.universal.vanilla.renderer;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.huskuraft.universal.api.core.BlockEntity;
import dev.huskuraft.universal.api.core.BlockPosition;
import dev.huskuraft.universal.api.core.BlockState;
import dev.huskuraft.universal.api.core.ItemStack;
import dev.huskuraft.universal.api.core.World;
import dev.huskuraft.universal.api.gui.Typeface;
import dev.huskuraft.universal.api.renderer.BufferSource;
import dev.huskuraft.universal.api.renderer.LightTexture;
import dev.huskuraft.universal.api.renderer.MatrixStack;
import dev.huskuraft.universal.api.renderer.OverlayTexture;
import dev.huskuraft.universal.api.renderer.PreviewBlock;
import dev.huskuraft.universal.api.renderer.PreviewCamera;
import dev.huskuraft.universal.api.renderer.PreviewMesh;
import dev.huskuraft.universal.api.renderer.RenderLayer;
import dev.huskuraft.universal.api.renderer.Renderer;
import dev.huskuraft.universal.api.text.Text;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2fStack;

/**
 * 26.2 的渲染适配层。
 * <p>
 * 26.2 把渲染拆成了两套完全不同的体系：
 * <ul>
 *     <li><b>世界渲染（3D）</b>：{@link PoseStack} + {@link SubmitNodeCollector}。
 *         顶点不能直接写 —— 必须通过
 *         {@code submitCustomGeometry(PoseStack, RenderType, CustomGeometryRenderer)} 提交，
 *         真正的写入发生在回调给的 {@code VertexConsumer} 上（延迟执行）。</li>
 *     <li><b>GUI 渲染（2D）</b>：{@link GuiGraphicsExtractor}，矩阵是 2D 的
 *         {@link Matrix3x2fStack}，绘制直接调 {@code text/fill/blit/item}，
 *         已经没有「顶点缓冲」这一层了。</li>
 * </ul>
 * 两种上下文用两个静态工厂区分，见 {@link #forWorld(LevelRenderContext)} 与 {@link #forGui(GuiGraphicsExtractor)}。
 */
public class MinecraftRenderer extends Renderer {

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(MinecraftRenderer.class);

    private final Minecraft minecraftClient;
    private final MatrixStack matrixStack;
    private final MinecraftBufferSource bufferSource;
    private final GuiGraphicsExtractor graphics;
    private final SubmitNodeCollector collector;
    private final PoseStack poseStack;
    private final CameraRenderState cameraRenderState;
    private final Matrix3x2fStack matrix2d;
    /** {@code setRsShaderColor} 的全局着色，26.2 没有 shader color 了，改成乘到顶点色上。 */
    private final float[] tint = {1.0F, 1.0F, 1.0F, 1.0F};
    /** 最近一次设置的裁剪框（诊断用）。 */
    private static int[] lastScissor = null;

    /**
     * GUI / HUD 渲染（2D）。
     * <p>
     * 26.2 的 {@code GuiGraphicsExtractor} 依赖 {@code GuiRenderState}，而后者只能在屏幕渲染
     * pass 内部拿到，所以渲染器不能自己 new —— 必须由调用方把已有的实例传进来。
     *
     * @param graphics 26.2 的 {@link GuiGraphicsExtractor}
     */
    public MinecraftRenderer(GuiGraphicsExtractor graphics) {
        this.minecraftClient = Minecraft.getInstance();
        this.graphics = graphics;
        this.collector = null;
        this.poseStack = null;
        this.cameraRenderState = null;
        // 26.2 的 GUI 有两套绘制路径，矩阵必须**只**由我们这一侧统一施加一次：
        //  - 文字 / 物品 / tooltip：GuiGraphicsExtractor 自己会应用它内部的 pose，
        //    所以这里不能把我们的 2D 矩阵塞进它的 pose（否则矩形路径会重复应用），
        //    只能在 transformGui 里按我们的矩阵手工变换坐标。
        //  - 矩形 / 四边形 / 贴图：common-api 会把矩阵烘焙进顶点，
        //    再由 GuiGraphicsExtractor.blit 贴出来，同样只应用一次。
        this.matrix2d = MinecraftMatrixStack.createMatrix2d();
        this.matrixStack = new MinecraftMatrixStack(matrix2d);
        this.bufferSource = new MinecraftBufferSource(graphics, tint);
    }

    private MinecraftRenderer(PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraRenderState) {
        this.minecraftClient = Minecraft.getInstance();
        this.graphics = null;
        this.collector = collector;
        this.poseStack = poseStack;
        this.cameraRenderState = cameraRenderState;
        this.matrix2d = null;
        this.matrixStack = new MinecraftMatrixStack(poseStack);
        this.bufferSource = new MinecraftBufferSource(collector, poseStack, tint);
    }

    /**
     * 世界渲染（3D），只有矩阵栈与提交节点。
     *
     * @param poseStack 3D 的 {@link PoseStack}
     * @param collector {@link SubmitNodeCollector}
     */
    public MinecraftRenderer(PoseStack poseStack, SubmitNodeCollector collector) {
        this(poseStack, collector, null);
    }

    /**
     * 世界渲染（3D）。
     *
     * @param context fabric-rendering-v1 的 {@link LevelRenderContext}，提供 3D 的
     *                {@code poseStack()} 与 {@code submitNodeCollector()}
     */
    public static MinecraftRenderer forWorld(LevelRenderContext context) {
        var levelState = context.levelState();
        var cameraRenderState = levelState == null ? null : levelState.cameraRenderState;
        return new MinecraftRenderer(context.poseStack(), context.submitNodeCollector(), cameraRenderState);
    }

    /**
     * GUI / HUD 渲染（2D）。
     *
     * @param graphics 26.2 的 {@link GuiGraphicsExtractor}
     */
    public static MinecraftRenderer forGui(GuiGraphicsExtractor graphics) {
        return new MinecraftRenderer(graphics);
    }

    @Override
    public MatrixStack matrixStack() {
        return matrixStack;
    }

    @Override
    public BufferSource bufferSource() {
        return bufferSource;
    }

    @Override
    protected void enableScissor(int x, int y, int width, int height) {
        if (graphics != null) {
            // 【26.2 修正 · scissor 语义】26.2 的 enableScissor 收的是「角点 (x1,y1,x2,y2)」，
            // 而且是 GUI 逻辑坐标（y 向下），和原版 AbstractSelectionList 的用法一致。
            // 1.21.3 的 RenderSystem.enableScissor 是 (x, y, 宽, 高) + 物理像素 + 原点在左下。
            lastScissor = new int[]{x, y, width, height};
            graphics.enableScissor(x, y, x + width, y + height);
        }
        // 世界模式：26.2 的 scissor 是渲染通道状态（ScissorState），没有对外的公开入口
    }

    @Override
    protected void disableScissor() {
        if (graphics != null) {
            graphics.disableScissor();
        }
    }

    @Override
    public void setRsShaderColor(float red, float green, float blue, float alpha) {
        // 26.2 没有 RenderSystem.setShaderColor 了，改成把颜色乘到顶点色上
        tint[0] = red;
        tint[1] = green;
        tint[2] = blue;
        tint[3] = alpha;
    }

    @Override
    public void flush() {
        bufferSource.end();
    }

    /**
     * GUI 侧的精灵绘制。
     * <p>
     * 【26.2 修正 · 尺寸】26.2 的 {@code blitSprite} 参数语义是
     * {@code (x, y, width, height)}——<b>宽高</b>，不是 {@code (x1, y1, x2, y2)}。
     * 原版 {@code AbstractButton#extractDefaultSprite} 就是
     * {@code blitSprite(GUI_TEXTURED, <精灵 id>, getX(), getY(), getWidth(), getHeight(), alpha)}。
     * 之前按角点传参，导致按钮宽度变成 {@code x + width}、高度变成 {@code y + height}，
     * 整个面板被撑爆（就是「GUI 全部都变大了」那个现象）。
     * <p>
     * 【26.2 修正 · 九宫格】vanilla GUI 精灵改成了九宫格：例如按钮精灵
     * {@code minecraft:widget/button} 在图集里是 200x20 的带边框贴图，
     * 直接按 UV 拉伸会把中间糊成条纹。走原生 {@code blitSprite} 由精灵元数据处理缩放规则。
     * 非图集贴图（本 mod 自己的图标、面板底色）仍走原来的 UV 拉伸路径。
     */
    @Override
    public void renderSprite(dev.huskuraft.universal.api.texture.TextureSprite sprite, int x, int y, int width, int height) {
        if (graphics != null && sprite.name() != null) {
            var id = (net.minecraft.resources.Identifier) sprite.name().reference();
            if (id != null && isGuiAtlasSprite(id)) {
                // 矩阵可能带缩放（面板弹出动画），所以两个角点都要变换，尺寸由变换后的差值算
                var topLeft = transformGui(x, y);
                var bottomRight = transformGui(x + width, y + height);
                try {
                    // 直接路径绘制前必须先把顶点路径的批次刷进去，否则顺序会反过来（见 flushPending 说明）
                    flushPending();
                    graphics.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
                            id,
                            topLeft[0], topLeft[1],
                            bottomRight[0] - topLeft[0], bottomRight[1] - topLeft[1],
                            tint[3]);
                    return;
                } catch (Throwable throwable) {
                    // 精灵 id 在 GUI 图集里查不到（会抛 MissingSprite）→ 退回 UV 拉伸
                    LOGGER.warn("blitSprite failed for {}: {}", id, throwable.toString());
                }
            }
        }
        super.renderSprite(sprite, x, y, width, height);
    }

    /**
     * 判断这个精灵 id 是否在 vanilla 的 GUI 图集里。
     * <p>
     * 只有图集里的精灵才能用 {@code blitSprite} 画；本 mod 自己的贴图（图标、面板底色）
     * 必须走 UV 拉伸路径。
     */
    private boolean isGuiAtlasSprite(net.minecraft.resources.Identifier id) {
        try {
            var atlasManager = Minecraft.getInstance().getAtlasManager();
            return atlasManager.get(new net.minecraft.client.resources.model.sprite.SpriteId(
                    net.minecraft.client.renderer.Sheets.GUI_SHEET, id)) != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    protected int renderTextInternal(Typeface typeface, Text text, int x, int y, int color, int backgroundColor, boolean shadow, boolean seeThrough, int lightMap) {
        var font = (Font) typeface.reference();
        var component = (Component) text.reference();
        // 【26.2 修正 · 文字颜色模型】26.2 的 Font 用「传入颜色的 alpha」+「样式里的 RGB」合成字形颜色
        // （Font$PreparedTextBuilder#getTextColor：alpha 取自传入色，rgb 取自 style.getColor()）。
        // 1.21.3 的字体 alpha 是独立字段，调用方习惯直接传 RGB（0xFFFFFF / 0x404040 …），
        // 照搬到 26.2 会因为 alpha=0 让整段文字隐形（面板标题、轮盘标签都是这样没的）。
        // 所以这里补上 alpha=0xFF，等价于 1.21.3 的「默认不透明」。
        var opaque = color | 0xFF000000;
        if (graphics != null) {
            // 【26.2 修正 · 绘制顺序】26.2 的 GUI 有两条绘制路径：
            //   顶点路径（矩形 / 四边形 / 贴图）：顶点先攒着，flush() 时才变成 GUI 渲染状态里的元素；
            //   直接路径（文字 / 物品 / tooltip / blitSprite）：立刻进入 GUI 渲染状态。
            // 两条路径混用时如果不先 flush，先画的文字会排在自己面板的贴图**前面** → 被面板底图盖住看不见
            // （表现就是「面板标题不显示」，而按钮标签正常 —— 按钮渲染时刚好触发了 flush）。
            flushPending();
            var position = transformGui(x, y);
            if (MinecraftBufferSource.DEBUG) {
                LOGGER.debug("[gui] text '{}' at ({},{}) color={} styleColor={} scissor={}", component.getString(),
                        position[0], position[1], Integer.toHexString(opaque),
                        component.getStyle().getColor() == null ? "null" : Integer.toHexString(component.getStyle().getColor().getValue()),
                        lastScissor == null ? "none" : Arrays.toString(lastScissor));
            }
            graphics.text(font, component, position[0], position[1], opaque, shadow);
            // 【26.2 修正 · 返回值语义】common-api 约定 renderText 返回「文本结束处的绝对 x」
            // （上游 1.21.3 的 Font#drawInBatch 返回的就是 x + width）。
            // 之前返回的是「文字宽度」，于是调用方拿它当光标位置用时就少了起点偏移：
            // 输入框里光标和选区高亮全跑到屏幕最左边去了。
            return x + font.width(component);
        }
        collector.submitText(
                poseStack,
                x,
                y,
                component.getVisualOrderText(),
                shadow,
                seeThrough ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL,
                lightMap,
                opaque,
                backgroundColor,
                0
        );
        return x + font.width(component);
    }

    /**
     * 文本选区高亮。
     * <p>
     * 26.2 原版 {@code EditBox} 用的是 {@code GuiGraphicsExtractor#textHighlight}：
     * 走 {@code GUI_TEXT_HIGHLIGHT} 管线（**加色混合**），
     * 所以蓝色是「加」在底上的 —— 选中的文字仍然看得见。
     * <p>
     * 如果退回普通的 {@code fill}（普通 alpha 混合），蓝色会把选中的文字整段盖住。
     */
    @Override
    public void renderTextHighlight(int x1, int y1, int x2, int y2, int color, boolean invert) {
        if (graphics == null) {
            super.renderTextHighlight(x1, y1, x2, y2, color, invert);
            return;
        }
        // 直接路径：绘制前先刷掉顶点路径攒下的批次，否则顺序会反过来（见 flushPending 说明）
        flushPending();
        var a = transformGui(x1, y1);
        var b = transformGui(x2, y2);
        // 归一化成「左 <= 右、上 <= 下」：原版矩形填充的绕序是这样，反了会被背面剔除
        var left = Math.min(a[0], b[0]);
        var right = Math.max(a[0], b[0]);
        var top = Math.min(a[1], b[1]);
        var bottom = Math.max(a[1], b[1]);
        if (invert) {
            graphics.fill(RenderPipelines.GUI_INVERT, left, top, right, bottom, -1);
        }
        graphics.fill(RenderPipelines.GUI_TEXT_HIGHLIGHT, left, top, right, bottom, color);
    }

    @Override
    public void renderItem(ItemStack stack, int x, int y) {
        if (graphics == null) {
            // 世界模式下 26.2 的物品渲染要走 ItemModelResolver + submitItem，无法直接复用，暂不支持
            return;
        }
        flushPending();
        var position = transformGui(x, y);
        graphics.item((net.minecraft.world.item.ItemStack) stack.reference(), position[0], position[1]);
    }

    @Override
    public void renderItemScaled(ItemStack stack, int x, int y, float scale) {
        if (graphics == null) {
            return;
        }
        flushPending();
        // 26.2 的 GUI 物品渲染会带上当时的位姿（GuiItemRenderState.pose），
        // 位姿同时作用于物品模型和它的位置 —— 所以这里推一个「平移到 (x,y) 再缩放」的位姿，
        // 物品就会以 (x,y) 为左上角、按 scale 缩放。
        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(scale, scale);
        graphics.item((net.minecraft.world.item.ItemStack) stack.reference(), 0, 0);
        pose.popMatrix();
    }

    @Override
    public void renderTooltip(Typeface typeface, List<Text> list, int x, int y) {
        if (graphics == null) {
            return;
        }
        flushPending();
        var font = (Font) typeface.reference();
        var components = list.stream().map(text -> (Component) text.reference()).toList();
        var position = transformGui(x, y);
        graphics.setTooltipForNextFrame(font, components, Optional.empty(), position[0], position[1]);
    }

    /**
     * 方块模型细分出来的一个面（贴图 + 顶点）。
     * <p>
     * 位置是方块局部坐标（0~1），UV 是贴图内坐标（0~1），
     * 图集矩形是这个面用的贴图在方块图集里的范围。
     */
    private record ModelFace(float[] positions, float[] uvs, float u0, float v0, float u1, float v1,
                             int tint, float nx, float ny, float nz) {
    }

    /** 方块状态 → 它的模型面（跨帧缓存，资源重载后清空）。 */
    private static final java.util.Map<net.minecraft.world.level.block.state.BlockState, List<ModelFace>> MODEL_FACE_CACHE = new java.util.HashMap<>();
    /** 上次用的方块图集纹理；资源重载后图集会换，缓存里的 UV 矩形就失效了。 */
    private static Object lastBlockAtlasView;

    @Override
    public PreviewMesh buildPreviewMesh(List<PreviewBlock> blocks) {
        if (blocks == null || blocks.isEmpty()) {
            return PreviewMesh.EMPTY;
        }

        var minecraft = Minecraft.getInstance();
        var atlasTexture = minecraft.getTextureManager().getTexture(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS);
        if (lastBlockAtlasView != atlasTexture.getTextureView()) {
            lastBlockAtlasView = atlasTexture.getTextureView();
            MODEL_FACE_CACHE.clear();
        }
        var modelManager = minecraft.getModelManager();
        var random = net.minecraft.util.RandomSource.createThreadLocalInstance(0);

        // 结构中心（用方块格的范围，格心在 +0.5）
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        // 位置 → 方块状态：用来做邻接查询（判断某个面被相邻方块挡住没有）
        var lookup = new java.util.HashMap<Long, net.minecraft.world.level.block.state.BlockState>(blocks.size() * 2);
        for (var block : blocks) {
            var state = (net.minecraft.world.level.block.state.BlockState) block.state().reference();
            if (state == null) {
                continue;
            }
            lookup.put(packPosition(block.position().x(), block.position().y(), block.position().z()), state);
            minX = Math.min(minX, block.position().x());
            maxX = Math.max(maxX, block.position().x());
            minY = Math.min(minY, block.position().y());
            maxY = Math.max(maxY, block.position().y());
            minZ = Math.min(minZ, block.position().z());
            maxZ = Math.max(maxZ, block.position().z());
        }
        if (lookup.isEmpty()) {
            return PreviewMesh.EMPTY;
        }
        var centerX = (minX + maxX + 1) / 2.0;
        var centerY = (minY + maxY + 1) / 2.0;
        var centerZ = (minZ + maxZ + 1) / 2.0;

        // 1) 逐块取模型面，并剔除被相邻方块挡住的面
        var faces = new java.util.ArrayList<PreviewMesh.PreviewFace>();
        int totalFaces = 0;
        int culledFaces = 0;
        for (var block : blocks) {
            var state = (net.minecraft.world.level.block.state.BlockState) block.state().reference();
            if (state == null) {
                continue;
            }
            for (var face : getModelFaces(state, modelManager, random)) {
                totalFaces++;
                // 【邻接面剔除】和原版渲染区块时是同一套判定：
                // 某个方向上相邻的方块若把这个面完全挡住，这个面就不画。
                // 实心结构里这一项能砍掉约 90% 的面（内部面根本看不见）。
                var direction = directionOf(face.nx(), face.ny(), face.nz());
                if (direction == null) {
                    continue;
                }
                var neighbor = lookup.get(packPosition(
                        block.position().x() + direction.getStepX(),
                        block.position().y() + direction.getStepY(),
                        block.position().z() + direction.getStepZ()));
                if (neighbor != null && !net.minecraft.world.level.block.Block.shouldRenderFace(state, neighbor, direction)) {
                    culledFaces++;
                    continue;
                }
                // 顶点位置平移到方块在结构里的位置（相对结构中心）
                var positions = new float[12];
                for (int i = 0; i < 4; i++) {
                    positions[i * 3] = (float) (block.position().x() + face.positions()[i * 3] - centerX);
                    positions[i * 3 + 1] = (float) (block.position().y() + face.positions()[i * 3 + 1] - centerY);
                    positions[i * 3 + 2] = (float) (block.position().z() + face.positions()[i * 3 + 2] - centerZ);
                }
                faces.add(new PreviewMesh.PreviewFace(
                        positions, face.uvs().clone(),
                        face.u0(), face.v0(), face.u1(), face.v1(),
                        face.tint(),
                        face.nx(), face.ny(), face.nz()));
            }
        }

        if (MinecraftBufferSource.DEBUG) {
            LOGGER.info("[preview] blocks={} faces={} culled={} visible={}",
                    lookup.size(), totalFaces, culledFaces, faces.size());
        }

        return new PreviewMesh(
                List.copyOf(faces),
                (float) centerX, (float) centerY, (float) centerZ,
                (float) minX, (float) minY, (float) minZ,
                (float) maxX, (float) maxY, (float) maxZ);
    }

    /** 把方块坐标打包成一个 long，用作位置索引表的键。 */
    private static long packPosition(int x, int y, int z) {
        return ((long) x & 0x3FFFFFFL) << 38 | ((long) z & 0x3FFFFFFL) << 12 | ((long) y & 0xFFFL);
    }

    /** 面的法线 → 方向（用于邻接查询）。 */
    private static net.minecraft.core.Direction directionOf(float nx, float ny, float nz) {
        for (var direction : net.minecraft.core.Direction.values()) {
            if (direction.getStepX() == nx && direction.getStepY() == ny && direction.getStepZ() == nz) {
                return direction;
            }
        }
        return null;
    }

    @Override
    public void renderModelPreview(PreviewMesh mesh, PreviewCamera camera, int x, int y, int width, int height) {
        if (graphics == null || mesh == null || mesh.isEmpty() || width <= 0 || height <= 0) {
            return;
        }
        flushPending();

        var minecraft = Minecraft.getInstance();
        var atlasTexture = minecraft.getTextureManager().getTexture(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS);
        if (lastBlockAtlasView != atlasTexture.getTextureView()) {
            lastBlockAtlasView = atlasTexture.getTextureView();
            MODEL_FACE_CACHE.clear();
        }
        var textureSetup = net.minecraft.client.gui.render.TextureSetup.singleTexture(atlasTexture.getTextureView(), atlasTexture.getSampler());

        var yaw = Math.toRadians(camera.yaw());
        var pitch = Math.toRadians(camera.pitch());
        float cosY = (float) Math.cos(yaw);
        float sinY = (float) Math.sin(yaw);
        float cosX = (float) Math.cos(pitch);
        float sinX = (float) Math.sin(pitch);

        // 1) 旋转到相机空间 + 背面剔除
        var faces = new java.util.ArrayList<PreviewMesh.PreviewFace>(mesh.faces().size());
        for (var face : mesh.faces()) {
            var positions = face.positions();
            var nx = face.nx() * cosY + face.nz() * sinY;
            var nz = -face.nx() * sinY + face.nz() * cosY;
            var ny = face.ny() * cosX - nz * sinX;
            nz = face.ny() * sinX + nz * cosX;
            // 背面剔除：法线背对相机的面不画
            if (nz <= 0.0F) {
                continue;
            }
            var rotated = new float[12];
            for (int i = 0; i < 4; i++) {
                var wx = positions[i * 3];
                var wy = positions[i * 3 + 1];
                var wz = positions[i * 3 + 2];
                // 绕 Y 轴
                var rx = wx * cosY + wz * sinY;
                var rz = -wx * sinY + wz * cosY;
                // 绕 X 轴
                var ry = wy * cosX - rz * sinX;
                rz = wy * sinX + rz * cosX;
                rotated[i * 3] = rx;
                rotated[i * 3 + 1] = ry;
                rotated[i * 3 + 2] = rz;
            }
            faces.add(new PreviewMesh.PreviewFace(
                    rotated, face.uvs(),
                    face.u0(), face.v0(), face.u1(), face.v1(),
                    face.tint(),
                    nx, ny, nz));
        }
        if (faces.isEmpty()) {
            return;
        }

        // 2) 旋转后的包围盒 → 缩放（1 = 正好铺满视口）
        float minSx = Float.MAX_VALUE, minSy = Float.MAX_VALUE, maxSx = -Float.MAX_VALUE, maxSy = -Float.MAX_VALUE;
        for (var face : faces) {
            var positions = face.positions();
            for (int i = 0; i < 4; i++) {
                minSx = Math.min(minSx, positions[i * 3]);
                maxSx = Math.max(maxSx, positions[i * 3]);
                minSy = Math.min(minSy, positions[i * 3 + 1]);
                maxSy = Math.max(maxSy, positions[i * 3 + 1]);
            }
        }
        var scale = Math.min(width / Math.max(1.0F, maxSx - minSx), height / Math.max(1.0F, maxSy - minSy)) * 0.92F * camera.zoom();
        scale = Math.max(scale, 0.01F);
        var originX = x + width / 2.0F + camera.panX();
        var originY = y + height / 2.0F + camera.panY();

        // 3) 投影成屏幕坐标并提交（画家算法：远的先画）
        //    裁剪矩形 = 预览区（经当前 2D 矩阵变换后）——
        //    自己提交的 GUI 元素只认自己的 scissorArea()，不吃 graphics.enableScissor 那套全局裁剪，
        //    所以缩放/平移把模型顶出预览区时，必须由元素自己裁掉。
        var scissorTopLeft = transformGuiFloat(x, y);
        var scissorBottomRight = transformGuiFloat(x + width, y + height);
        var scissorRect = new net.minecraft.client.gui.navigation.ScreenRectangle(
                (int) Math.floor(Math.min(scissorTopLeft[0], scissorBottomRight[0])),
                (int) Math.floor(Math.min(scissorTopLeft[1], scissorBottomRight[1])),
                (int) Math.ceil(Math.abs(scissorBottomRight[0] - scissorTopLeft[0])),
                (int) Math.ceil(Math.abs(scissorBottomRight[1] - scissorTopLeft[1])));

        faces.sort((a, b) -> Float.compare(depthOf(a), depthOf(b)));
        for (var face : faces) {
            var positions = face.positions();
            var uvs = face.uvs();
            var sx = new float[4];
            var sy = new float[4];
            for (int i = 0; i < 4; i++) {
                sx[i] = (float) (originX + positions[i * 3] * scale);
                sy[i] = (float) (originY - positions[i * 3 + 1] * scale);
            }
            // GUI 管线会背面剔除：绕序必须和原版矩形填充一致（屏幕 y 向下 → 有向面积为负），
            // 不一致就交换 v1 / v3（三角形带 (v0,v1,v2)(v2,v1,v3) 交换后正好反向）。
            if (signedArea(sx, sy) > 0) {
                var tmpX = sx[1];
                sx[1] = sx[3];
                sx[3] = tmpX;
                var tmpY = sy[1];
                sy[1] = sy[3];
                sy[3] = tmpY;
                var tmpU = uvs[2];
                uvs[2] = uvs[6];
                uvs[6] = tmpU;
                var tmpV = uvs[3];
                uvs[3] = uvs[7];
                uvs[7] = tmpV;
            }
            var color = face.tint();
            // 【26.2 修正 · 矩阵只施加一次】
            // 顶点路径的规矩（见类注释）：矩形/四边形这一侧由 common-api 把矩阵**烘焙进顶点**，
            // 提交的 pose 必须是单位矩阵，否则 GuiGraphicsExtractor 会再乘一次 →
            // 面板弹出动画的缩放/平移被施加两遍，模型会放大并偏移出预览区。
            // 这里按 transformGui 的做法手工把坐标变换到屏幕空间，pose 传单位矩阵。
            var tx = new float[4];
            var ty = new float[4];
            for (int i = 0; i < 4; i++) {
                var p = transformGuiFloat(sx[i], sy[i]);
                tx[i] = p[0];
                ty[i] = p[1];
            }
            GuiRenderStateAccess.get(graphics).addGuiElement(new QuadRenderState(
                    RenderPipelines.GUI_TEXTURED,
                    textureSetup,
                    new org.joml.Matrix3x2f(),
                    tx[0], ty[0], tx[1], ty[1], tx[2], ty[2], tx[3], ty[3],
                    color,
                    new float[]{
                            spriteU(face, uvs[0]), spriteV(face, uvs[1]),
                            spriteU(face, uvs[2]), spriteV(face, uvs[3]),
                            spriteU(face, uvs[4]), spriteV(face, uvs[5]),
                            spriteU(face, uvs[6]), spriteV(face, uvs[7])
                    },
                    scissorRect
            ));
        }
    }

    /** 四个屏幕坐标点的有向面积（屏幕 y 向下时，负数 = 与原版矩形填充同向）。 */
    private static float signedArea(float[] sx, float[] sy) {
        return (sx[1] - sx[0]) * (sy[2] - sy[0]) - (sx[2] - sx[0]) * (sy[1] - sy[0]);
    }

    private static float depthOf(PreviewMesh.PreviewFace face) {
        var positions = face.positions();
        float depth = 0.0F;
        for (int i = 0; i < 4; i++) {
            depth += positions[i * 3 + 2];
        }
        return depth / 4.0F;
    }

    /** 贴图内坐标（0~1）→ 方块图集里的 u。 */
    private static float spriteU(PreviewMesh.PreviewFace face, float u) {
        return face.u0() + (face.u1() - face.u0()) * u;
    }

    /** 贴图内坐标（0~1）→ 方块图集里的 v。 */
    private static float spriteV(PreviewMesh.PreviewFace face, float v) {
        return face.v0() + (face.v1() - face.v0()) * v;
    }

    /**
     * 逐面亮度：和 MC 渲染方块时一样（顶面最亮、底面最暗、两个侧面居中）。
     * <p>
     * 没有这层明暗，相邻的两个面颜色一样，模型看起来就是一块扁的板。
     */
    private static int shadeOf(net.minecraft.core.Direction direction) {
        var shade = switch (direction) {
            case DOWN -> 0.5F;
            case UP -> 1.0F;
            case NORTH, SOUTH -> 0.8F;
            default -> 0.6F;
        };
        var value = Math.round(shade * 255.0F);
        return 0xFF000000 | (value << 16) | (value << 8) | value;
    }

    /**
     * 取一个方块状态的模型面（带缓存）。
     * <p>
     * 用的是 MC 自己的方块模型细分（{@code BlockStateModelPart#getQuads}），
     * 所以贴图、旋转、朝向都和世界里的一模一样。
     */
    private static List<ModelFace> getModelFaces(net.minecraft.world.level.block.state.BlockState state,
                                                  net.minecraft.client.resources.model.ModelManager modelManager,
                                                  net.minecraft.util.RandomSource random) {
        return MODEL_FACE_CACHE.computeIfAbsent(state, key -> {
            var result = new java.util.ArrayList<ModelFace>();
            try {
                var model = modelManager.getBlockStateModelSet().get(key);
                if (model == null) {
                    return result;
                }
                var parts = new java.util.ArrayList<net.minecraft.client.renderer.block.dispatch.BlockStateModelPart>();
                model.collectParts(random, parts);
                for (var part : parts) {
                    for (var direction : net.minecraft.core.Direction.values()) {
                        for (var quad : part.getQuads(direction)) {
                            var positions = new float[12];
                            var uvs = new float[8];
                            for (int i = 0; i < 4; i++) {
                                var position = quad.position(i);
                                positions[i * 3] = position.x();
                                positions[i * 3 + 1] = position.y();
                                positions[i * 3 + 2] = position.z();
                                var packed = quad.packedUV(i);
                                uvs[i * 2] = net.minecraft.client.model.geom.builders.UVPair.unpackU(packed);
                                uvs[i * 2 + 1] = net.minecraft.client.model.geom.builders.UVPair.unpackV(packed);
                            }
                            var sprite = quad.materialInfo().sprite();
                            result.add(new ModelFace(positions, uvs,
                                    sprite.getU0(), sprite.getV0(), sprite.getU1(), sprite.getV1(),
                                    shadeOf(direction),
                                    direction.getStepX(), direction.getStepY(), direction.getStepZ()));
                        }
                    }
                }
            } catch (Throwable throwable) {
                LOGGER.debug("Failed to build model faces for {}", key, throwable);
            }
            return result;
        });
    }

    /**
     * 把顶点路径攒下的顶点先刷进 GUI 渲染状态。
     * <p>
     * 26.2 的 GUI 是「状态机」：顶点路径要等 {@code flush()} 才会变成渲染状态里的元素，
     * 而文字 / 物品 / tooltip 这些直接路径是立刻进去的。
     * 不先刷的话，绘制顺序会反 —— 先画的文字被后画的贴图盖住（面板标题消失就是这么来的）。
     */
    private void flushPending() {
        this.flush();
    }

    @Override
    public void renderBlockState(RenderLayer renderLayer, World world, BlockPosition blockPosition, BlockState blockState) {
        if (collector == null) {
            // GUI 侧画不了 3D 方块预览
            return;
        }
        var state = (net.minecraft.world.level.block.state.BlockState) blockState.reference();
        var renderState = new BlockModelRenderState();
        var resolver = new BlockModelResolver(minecraftClient.getModelManager());
        resolver.update(renderState, state, BlockDisplayContext.create());
        // 【26.2 修正 · 坐标叠加】
        // 26.2 的方块模型是「局部坐标」（0..1），提交时位姿必须已经在方块位置 —— 这是调用方的责任
        // （上游 BlockOperationRenderer 会先 translate(方块位置 - 相机)）。
        // 之前这里又叠了一次「方块绝对位置」，净位移变成 2×方块位置 - 相机：
        // 世界坐标越大偏得越远（近原点看不出来），方块预览就飞到几千格外去了。
        // 参考原版 26.2：LevelRenderer#submitBlockEntities 也是「调用方先平移，再 submit」。
        renderState.submit(poseStack, collector, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
    }

    @Override
    public void renderBlockEntity(RenderLayer renderLayer, World world, BlockPosition blockPosition, BlockEntity blockEntity) {
        if (collector == null) {
            return;
        }
        var entity = (net.minecraft.world.level.block.entity.BlockEntity) blockEntity.reference();
        var dispatcher = minecraftClient.getBlockEntityRenderDispatcher();
        var crumbling = new ModelFeatureRenderer.CrumblingOverlay(0, poseStack.last());
        var state = dispatcher.tryExtractRenderState(entity, 0.0F, crumbling, false);
        if (state == null) {
            return;
        }
        // 和 renderBlockState 一样：位姿由调用方摆好（方块位置 - 相机），
        // 方块实体渲染器本身是画局部坐标的，这里不能再叠一次位置。
        dispatcher.submit(state, poseStack, collector, cameraRenderState);
    }

    /**
     * GUI 模式下把 common-api 的逻辑坐标按当前 2D 矩阵变换一次。
     * <p>
     * 文字 / 物品 / tooltip 这一侧不能把矩阵塞进 {@code GuiGraphicsExtractor} 的 pose
     * （矩形那一侧 common-api 已经把矩阵烘焙进顶点了，再让 extractor 应用一次就会重复），
     * 所以这里手工变换一次，保证两条路径都只应用一次矩阵。
     */
    private int[] transformGui(int x, int y) {
        if (matrix2d == null) {
            return new int[]{x, y};
        }
        // JOML 的 Matrix3x2f 是列主序：x' = m00*x + m10*y + m20，y' = m01*x + m11*y + m21
        var fx = (float) x;
        var fy = (float) y;
        var nx = matrix2d.m00() * fx + matrix2d.m10() * fy + matrix2d.m20();
        var ny = matrix2d.m01() * fx + matrix2d.m11() * fy + matrix2d.m21();
        return new int[]{Math.round(nx), Math.round(ny)};
    }

    /**
     * {@link #transformGui(int, int)} 的浮点版本（模型预览的顶点坐标不能取整）。
     */
    private float[] transformGuiFloat(float x, float y) {
        if (matrix2d == null) {
            return new float[]{x, y};
        }
        var nx = matrix2d.m00() * x + matrix2d.m10() * y + matrix2d.m20();
        var ny = matrix2d.m01() * x + matrix2d.m11() * y + matrix2d.m21();
        return new float[]{nx, ny};
    }

}
