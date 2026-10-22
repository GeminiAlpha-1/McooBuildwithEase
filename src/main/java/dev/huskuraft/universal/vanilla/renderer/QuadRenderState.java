package dev.huskuraft.universal.vanilla.renderer;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2fc;

/**
 * 一个任意四边形的 GUI 渲染元素（拆成两个三角形）。
 * <p>
 * <b>为什么需要它</b>：26.2 的 {@code GuiGraphicsExtractor} 只剩「轴对齐矩形 + 贴图 blit」，
 * 没有多边形填充；而 1.21.3 是靠 {@code MultiBufferSource} 直接画四边形的
 * （径向菜单的每个扇形就是一个梯形）。
 * <p>
 * 之前用「单位矩形 + 2D 仿射矩阵」近似：仿射变换只能把矩形变成<b>平行四边形</b>，
 * 而梯形的两条对边不等长 → 每个小块的对角点都有误差（4×4 切分时约 0.9 逻辑像素）。
 * 相邻小块的共享边因此错开 0.9 像素 → 出现缝（漏出背景）或重叠（半透明叠两次变深），
 * 视觉上就是扇形上一道道细划痕、彩色段上的黑点。
 * <p>
 * 这里直接提交两个三角形，形状与 1.21.3 完全一致，没有任何近似误差。
 */
public final class QuadRenderState implements GuiElementRenderState {

    private final RenderPipeline pipeline;
    private final TextureSetup textureSetup;
    private final Matrix3x2fc pose;
    private final float x0;
    private final float y0;
    private final float x1;
    private final float y1;
    private final float x2;
    private final float y2;
    private final float x3;
    private final float y3;
    private final int color;
    /** 每个顶点的贴图坐标（4 个顶点 × 2 个 float）；为 null 表示不贴图。 */
    private final float[] uvs;
    /** 裁剪矩形（屏幕坐标）；null 表示不裁剪。 */
    private final ScreenRectangle scissor;
    private final ScreenRectangle bounds;

    public QuadRenderState(RenderPipeline pipeline, TextureSetup textureSetup, Matrix3x2fc pose,
                           float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3,
                           int color) {
        this(pipeline, textureSetup, pose, x0, y0, x1, y1, x2, y2, x3, y3, color, null, null);
    }

    /**
     * 带贴图的四边形。
     *
     * @param uvs 4 个顶点的贴图坐标（u0,v0,u1,v1,u2,v2,u3,v3），要和顶点顺序一一对应
     */
    public QuadRenderState(RenderPipeline pipeline, TextureSetup textureSetup, Matrix3x2fc pose,
                           float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3,
                           int color, float[] uvs) {
        this(pipeline, textureSetup, pose, x0, y0, x1, y1, x2, y2, x3, y3, color, uvs, null);
    }

    /**
     * 带贴图 + 带裁剪的四边形。
     *
     * @param scissor 裁剪矩形（屏幕坐标）；null 表示不裁剪。
     */
    public QuadRenderState(RenderPipeline pipeline, TextureSetup textureSetup, Matrix3x2fc pose,
                           float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3,
                           int color, float[] uvs, ScreenRectangle scissor) {
        this.pipeline = pipeline;
        this.textureSetup = textureSetup;
        this.pose = pose;
        this.x0 = x0;
        this.y0 = y0;
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.x3 = x3;
        this.y3 = y3;
        this.color = color;
        this.uvs = uvs;
        this.scissor = scissor;
        this.bounds = bounds(x0, y0, x1, y1, x2, y2, x3, y3);
    }

    /**
     * 顶点必须按 <b>三角形带（triangle strip）</b> 的顺序发，不是三角形列表。
     * <p>
     * GUI 管线用的是带状拓扑（{@code RenderPipelines.GUI}）—— 原版的矩形填充就是发 4 个顶点
     * 凑成 2 个三角形（TL, BL, BR, TR）。带状拓扑会把 4 个顶点解释成
     * (v0,v1,v2) 与 (v2,v1,v3) 两个三角形，所以这里也发 4 个顶点。
     * <p>
     * 绕序必须和原版矩形填充一致，否则会被背面剔除（画了但看不见）。
     * 原版发出的是 (x0,y0) → (x0,y1) → (x1,y1) → (x1,y0)，
     * 在屏幕坐标系（y 向下）里是有向面积为负的绕序；调用方保证顶点顺序与之一致。
     */
    @Override
    public void buildVertices(com.mojang.blaze3d.vertex.VertexConsumer consumer) {
        if (uvs == null) {
            consumer.addVertexWith2DPose(pose, x0, y0).setColor(color);
            consumer.addVertexWith2DPose(pose, x1, y1).setColor(color);
            consumer.addVertexWith2DPose(pose, x2, y2).setColor(color);
            consumer.addVertexWith2DPose(pose, x3, y3).setColor(color);
            return;
        }
        consumer.addVertexWith2DPose(pose, x0, y0).setColor(color).setUv(uvs[0], uvs[1]);
        consumer.addVertexWith2DPose(pose, x1, y1).setColor(color).setUv(uvs[2], uvs[3]);
        consumer.addVertexWith2DPose(pose, x2, y2).setColor(color).setUv(uvs[4], uvs[5]);
        consumer.addVertexWith2DPose(pose, x3, y3).setColor(color).setUv(uvs[6], uvs[7]);
    }

    @Override
    public RenderPipeline pipeline() {
        return pipeline;
    }

    @Override
    public TextureSetup textureSetup() {
        return textureSetup;
    }

    @Override
    public ScreenRectangle scissorArea() {
        // GUI 渲染状态里允许 null（表示不裁剪）；径向菜单本身也不开裁剪。
        return scissor;
    }

    @Override
    public ScreenRectangle bounds() {
        return bounds;
    }

    private static ScreenRectangle bounds(float x0, float y0, float x1, float y1,
                                          float x2, float y2, float x3, float y3) {
        var minX = (float) Math.min(Math.min(x0, x1), Math.min(x2, x3));
        var minY = (float) Math.min(Math.min(y0, y1), Math.min(y2, y3));
        var maxX = (float) Math.max(Math.max(x0, x1), Math.max(x2, x3));
        var maxY = (float) Math.max(Math.max(y0, y1), Math.max(y2, y3));
        return new ScreenRectangle((int) minX, (int) minY, (int) (maxX - minX), (int) (maxY - minY));
    }
}
