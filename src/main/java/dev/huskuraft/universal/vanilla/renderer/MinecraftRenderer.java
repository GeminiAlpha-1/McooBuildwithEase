package dev.huskuraft.universal.vanilla.renderer;

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
import dev.huskuraft.universal.api.renderer.RenderLayer;
import dev.huskuraft.universal.api.renderer.Renderer;
import dev.huskuraft.universal.api.text.Text;
import dev.huskuraft.universal.vanilla.core.MinecraftConvertor;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
            graphics.enableScissor(x, y, width, height);
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
            var position = transformGui(x, y);
            graphics.text(font, component, position[0], position[1], opaque, shadow);
            return font.width(component);
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
        return font.width(component);
    }

    @Override
    public void renderItem(ItemStack stack, int x, int y) {
        if (graphics == null) {
            // 世界模式下 26.2 的物品渲染要走 ItemModelResolver + submitItem，无法直接复用，暂不支持
            return;
        }
        var position = transformGui(x, y);
        graphics.item((net.minecraft.world.item.ItemStack) stack.reference(), position[0], position[1]);
    }

    @Override
    public void renderTooltip(Typeface typeface, List<Text> list, int x, int y) {
        if (graphics == null) {
            return;
        }
        var font = (Font) typeface.reference();
        var components = list.stream().map(text -> (Component) text.reference()).toList();
        var position = transformGui(x, y);
        graphics.setTooltipForNextFrame(font, components, Optional.empty(), position[0], position[1]);
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
        var position = MinecraftConvertor.toPlatformBlockPosition(blockPosition);
        poseStack.pushPose();
        poseStack.translate(position.getX(), position.getY(), position.getZ());
        renderState.submit(poseStack, collector, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        poseStack.popPose();
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

}
