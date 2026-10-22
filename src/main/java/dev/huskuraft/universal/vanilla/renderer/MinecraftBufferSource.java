package dev.huskuraft.universal.vanilla.renderer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import dev.huskuraft.universal.api.renderer.BufferSource;
import dev.huskuraft.universal.api.renderer.RenderLayer;
import dev.huskuraft.universal.api.renderer.VertexBuffer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;

/**
 * 26.2 的顶点缓冲源。
 * <p>
 * 1.21.3 靠 {@code MultiBufferSource.BufferSource} 按 RenderType 分桶、{@code endBatch()} 一次性提交；
 * 26.2 改成「提交节点」体系：{@code SubmitNodeCollector#submitCustomGeometry(PoseStack, RenderType,
 * CustomGeometryRenderer)} 才是真正的入口，而且写入是<b>延迟</b>的。
 * <p>
 * 这里保留 common-api 的立即模式语义：顶点先写进 {@link MinecraftVertexBuffer} 记录下来的列表，
 * {@link #end()} 时再按 26.2 的 {@code RenderType} 分组提交。
 * <ul>
 *     <li>世界渲染 → {@code submitCustomGeometry}，回放顶点到回调给的 {@code VertexConsumer}。</li>
 *     <li>GUI 渲染 → 26.2 的 GUI 走 {@link GuiGraphicsExtractor}，没有顶点缓冲一说，
 *         这里把记录下来的四边形还原成 {@code fill} / {@code fillGradient} / {@code blit}。</li>
 * </ul>
 */
public class MinecraftBufferSource implements BufferSource {

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(MinecraftBufferSource.class);
    /** 临时诊断开关。 */
    public static final boolean DEBUG = Boolean.getBoolean("mcoo.debugGui");

    /** 一批待提交的顶点（按 26.2 的 RenderType 分组）。 */
    public static class Batch {

        private final RenderType renderType;
        private final Identifier texture;
        private final List<MinecraftVertexBuffer.Vertex> vertices = new ArrayList<>();

        Batch(RenderType renderType, Identifier texture) {
            this.renderType = renderType;
            this.texture = texture;
        }

        public RenderType renderType() {
            return renderType;
        }

        public Identifier texture() {
            return texture;
        }

        public List<MinecraftVertexBuffer.Vertex> vertices() {
            return vertices;
        }

    }

    private final SubmitNodeCollector collector;
    private final PoseStack poseStack;
    private final GuiGraphicsExtractor graphics;
    private final float[] tint;
    /** 26.2 的 GUI 渲染状态：用来提交自定义的 GUI 元素（任意四边形）。拿不到就退回仿射近似。 */
    private final net.minecraft.client.renderer.state.gui.GuiRenderState guiRenderState;
    private final Map<RenderType, Batch> batches = new LinkedHashMap<>();

    public MinecraftBufferSource(SubmitNodeCollector collector, PoseStack poseStack, float[] tint) {
        this.collector = collector;
        this.poseStack = poseStack;
        this.graphics = null;
        this.tint = tint;
        this.guiRenderState = null;
    }

    public MinecraftBufferSource(GuiGraphicsExtractor graphics, float[] tint) {
        this.collector = null;
        this.poseStack = null;
        this.graphics = graphics;
        this.tint = tint;
        this.guiRenderState = GuiRenderStateAccess.get(graphics);
    }

    @Override
    public Object refs() {
        return collector != null ? collector : graphics;
    }

    @Override
    public VertexBuffer getBuffer(RenderLayer renderLayer) {
        var batch = batch(renderLayer);
        return new MinecraftVertexBuffer(batch.vertices);
    }

    @Override
    public void end() {
        if (batches.isEmpty()) {
            return;
        }
        if (collector != null) {
            for (var batch : batches.values()) {
                if (batch.vertices.isEmpty()) {
                    continue;
                }
                collector.submitCustomGeometry(poseStack, batch.renderType, (pose, consumer) -> replay(batch, consumer));
            }
        } else if (graphics != null) {
            for (var batch : batches.values()) {
                replayGui(batch);
            }
        }
        batches.clear();
    }

    private Batch batch(RenderLayer renderLayer) {
        var refs = renderLayer.refs();
        var renderType = refs instanceof RenderType type ? type : RenderTypes.debugFilledBox();
        var texture = renderLayer instanceof MinecraftRenderLayer layer ? layer.texture() : null;
        return batches.computeIfAbsent(renderType, type -> new Batch(type, texture));
    }

    /**
     * 把记录下来的顶点重放进 26.2 的 {@link VertexConsumer}。
     * <p>
     * 26.2 的 {@code BufferBuilder} 按顶点格式的元素下标写入，属性没写就留 0，
     * 所以这里只写目标格式里真实存在的元素（多写会直接抛异常）。
     */
    private void replay(Batch batch, VertexConsumer consumer) {
        var format = batch.renderType.format();
        var color = format.contains(DefaultVertexFormat.COLOR_SEMANTIC_NAME);
        var uv0 = format.contains(DefaultVertexFormat.UV0_SEMANTIC_NAME);
        var uv1 = format.contains(DefaultVertexFormat.UV1_SEMANTIC_NAME);
        var uv2 = format.contains(DefaultVertexFormat.UV2_SEMANTIC_NAME);
        var normal = format.contains(DefaultVertexFormat.NORMAL_SEMANTIC_NAME);
        var lineWidth = format.contains(DefaultVertexFormat.LINE_WIDTH_SEMANTIC_NAME);

        for (var vertex : batch.vertices) {
            consumer.addVertex(vertex.x(), vertex.y(), vertex.z());
            if (color) {
                consumer.setColor(tint(vertex.red(), 0), tint(vertex.green(), 1), tint(vertex.blue(), 2), tint(vertex.alpha(), 3));
            }
            if (uv0) {
                consumer.setUv(vertex.u(), vertex.v());
            }
            if (uv1) {
                consumer.setUv1(vertex.u1(), vertex.v1());
            }
            if (uv2) {
                consumer.setUv2(vertex.u2(), vertex.v2());
            }
            if (normal) {
                if ((vertex.attributes() & MinecraftVertexBuffer.ATTRIBUTE_NORMAL) != 0) {
                    consumer.setNormal(vertex.nx(), vertex.ny(), vertex.nz());
                } else {
                    consumer.setNormal(0.0F, 0.0F, 1.0F);
                }
            }
            if (lineWidth) {
                // 26.2 把线宽变成了顶点属性，不写就是 0（线会消失）。
                consumer.setLineWidth(1.0F);
            }
        }
    }

    /**
     * GUI 侧：26.2 的 GUI 绘制不再经过顶点缓冲，这里把记录下来的四边形还原成
     * {@link GuiGraphicsExtractor} 的调用。
     * <p>
     * 【26.2 限制】26.2 的 GUI 只剩「轴对齐矩形 + 贴图 blit」，没有多边形填充。
     * 1.21.3 的 {@code MultiBufferSource} 能直接画任意四边形（轮盘菜单的扇形就靠它），
     * 所以这里额外用「2D 仿射矩阵」把矩形变成平行四边形来还原四边形，见 {@link #drawQuad}。
     */
    private void replayGui(Batch batch) {
        var vertices = batch.vertices;
        for (int i = 0; i + 3 < vertices.size(); i += 4) {
            var a = vertices.get(i);
            var b = vertices.get(i + 1);
            var c = vertices.get(i + 2);
            var d = vertices.get(i + 3);

            var x1 = (int) Math.floor(Math.min(Math.min(a.x(), b.x()), Math.min(c.x(), d.x())));
            var x2 = (int) Math.ceil(Math.max(Math.max(a.x(), b.x()), Math.max(c.x(), d.x())));
            var y1 = (int) Math.floor(Math.min(Math.min(a.y(), b.y()), Math.min(c.y(), d.y())));
            var y2 = (int) Math.ceil(Math.max(Math.max(a.y(), b.y()), Math.max(c.y(), d.y())));

            if (batch.texture() != null && (a.attributes() & MinecraftVertexBuffer.ATTRIBUTE_UV) != 0) {
                var u0 = Math.min(Math.min(a.u(), b.u()), Math.min(c.u(), d.u()));
                var u1 = Math.max(Math.max(a.u(), b.u()), Math.max(c.u(), d.u()));
                var v0 = Math.min(Math.min(a.v(), b.v()), Math.min(c.v(), d.v()));
                var v1 = Math.max(Math.max(a.v(), b.v()), Math.max(c.v(), d.v()));
                // 【26.2 修正 · UV 顺序】26.2 的 blit(Identifier, x0, y0, x1, y1, f, f, f, f) 四个浮点是
                // (uMin, uMax, vMin, vMax)——u 在前、v 在后（对应 BlitRenderState 的 u0/u1/v0/v1 字段）。
                // 之前按 (uMin, vMin, uMax, vMax) 传，等于把 u 区间压成 0 宽、v 区间压成 0 高，
                // 整张贴图只采到左上角一个像素 → 图标全部画不出来（面板底色因为是纯白才「看起来正常」）。
                graphics.blit(batch.texture(), x1, y1, x2, y2, u0, u1, v0, v1);
                continue;
            }

            var colorA = tint(color(a));
            var colorB = tint(color(b));
            var colorC = tint(color(c));
            var colorD = tint(color(d));
            if (colorA == colorB && colorB == colorC && colorC == colorD) {
                if (isAxisAligned(a, b, c, d)) {
                    if (DEBUG) {
                        LOGGER.info("[gui] fill rect {} {} {} {} color={}", x1, y1, x2, y2, Integer.toHexString(colorA));
                    }
                    graphics.fill(x1, y1, x2, y2, colorA);
                } else {
                    if (DEBUG) {
                        LOGGER.info("[gui] drawQuad a=({},{}) b=({},{}) c=({},{}) d=({},{}) color={}",
                                a.x(), a.y(), b.x(), b.y(), c.x(), c.y(), d.x(), d.y(), Integer.toHexString(colorA));
                    }
                    drawQuad(a, b, c, d, colorA);
                }
            } else {
                graphics.fillGradient(x1, y1, x2, y2, colorA, colorB);
            }
        }
    }

    /**
     * 判断这四个点是不是轴对齐的矩形（是的话直接用外接矩形画就完全准确）。
     */
    private static boolean isAxisAligned(MinecraftVertexBuffer.Vertex a, MinecraftVertexBuffer.Vertex b,
                                         MinecraftVertexBuffer.Vertex c, MinecraftVertexBuffer.Vertex d) {
        return distinct(a.x(), b.x(), c.x(), d.x()) <= 2 && distinct(a.y(), b.y(), c.y(), d.y()) <= 2;
    }

    private static int distinct(float a, float b, float c, float d) {
        var count = 1;
        if (b != a) {
            count++;
        }
        if (c != a && c != b) {
            count++;
        }
        if (d != a && d != b && d != c) {
            count++;
        }
        return count;
    }

    /**
     * 画一个任意四边形。
     * <p>
     * 26.2 的 GUI 没有多边形填充，但 {@code GuiGraphicsExtractor#fill} 会把
     * {@code pose}（2D 仿射矩阵）应用到矩形四个角上 —— 于是「矩阵变换后的矩形」就是一个平行四边形。
     * <p>
     * 仿射变换只能把矩形变成平行四边形，而任意四边形不是平行四边形，
     * 所以这里按双线性插值把四边形切成 N×N 个小块，每块用它的左上角平行四边形近似。
     * N=4 时误差在 1px 以内，肉眼看不出。
     */
    /**
     * 画一个任意四边形。
     * <p>
     * 26.2 的 GUI 没有多边形填充，只有轴对齐矩形 + 贴图 blit。
     * <p>
     * <b>优先走精确路径</b>：直接往 GUI 渲染状态里提交一个自定义元素（拆成两个三角形），
     * 形状和 1.21.3（用 {@code MultiBufferSource} 直接画四边形）完全一致。
     * <p>
     * <b>兜底走仿射近似</b>：{@code fill} 会把 {@code pose} 应用到矩形四角，
     * 所以「单位矩形 + 2D 仿射矩阵」能画出平行四边形；但仿射变换无法把矩形变成
     * 「两条对边不等长」的梯形（径向菜单的扇形就是梯形），
     * 只能再按双线性插值切成 N×N 小块、每块单独近似 —— 每块的对角点会有约 0.9 逻辑像素的误差
     * （4×4 时），相邻小块的共享边因此错开 → 扇形上出现一道道细划痕、彩色段上出现黑点。
     * 所以只有在拿不到 GUI 渲染状态时才用这条路。
     */
    private void drawQuad(MinecraftVertexBuffer.Vertex a, MinecraftVertexBuffer.Vertex b,
                          MinecraftVertexBuffer.Vertex c, MinecraftVertexBuffer.Vertex d, int color) {
        var p0 = new Vector2f(a.x(), a.y());
        var p1 = new Vector2f(b.x(), b.y());
        var p2 = new Vector2f(c.x(), c.y());
        var p3 = new Vector2f(d.x(), d.y());

        if (guiRenderState != null) {
            // 与原版矩形填充保持同样的绕序（屏幕坐标系 y 向下，有向面积为负），否则会被背面剔除
            if (signedArea(p0, p1, p2) > 0) {
                var swap = p1;
                p1 = p3;
                p3 = swap;
            }
            guiRenderState.addGuiElement(new QuadRenderState(
                    RenderPipelines.GUI,
                    TextureSetup.noTexture(),
                    new Matrix3x2f(graphics.pose()),
                    p0.x(), p0.y(), p1.x(), p1.y(), p2.x(), p2.y(), p3.x(), p3.y(),
                    color));
            return;
        }

        final int segments = 4;
        var pose = graphics.pose();
        // 当前 GUI 矩阵：我们的绘制要在它之后再叠加自己的仿射变换
        var current = new Matrix3x2f(pose);
        for (int iy = 0; iy < segments; iy++) {
            for (int ix = 0; ix < segments; ix++) {
                var u0 = ix / (float) segments;
                var u1 = (ix + 1) / (float) segments;
                var v0 = iy / (float) segments;
                var v1 = (iy + 1) / (float) segments;

                var q00 = bilinear(p0, p1, p2, p3, u0, v0);
                var q10 = bilinear(p0, p1, p2, p3, u1, v0);
                var q01 = bilinear(p0, p1, p2, p3, u0, v1);

                // 平行四边形：原点 q00，两条边分别指向 q10 / q01
                var e1 = new Vector2f(q10.x - q00.x, q10.y - q00.y);
                var e2 = new Vector2f(q01.x - q00.x, q01.y - q00.y);
                // 【绕序】26.2 的 GUI 填充是三角形面片，背面会被剔除。
                // 原版矩形填充的行列式是正的，而轮盘那种四边形的行列式是负的
                //（顶点顺序是「内圈起点 → 内圈终点 → 外圈终点 → 外圈起点」，在屏幕坐标系里是顺时针），
                // 直接套上去会把面片翻面 → 整块画不出来。
                // 这里检测行列式，为负就交换两条边，保证与原版矩形填充同向。
                if (e1.x * e2.y - e1.y * e2.x < 0) {
                    var swap = e1;
                    e1 = e2;
                    e2 = swap;
                }
                var transform = new Matrix3x2f(e1.x, e1.y, e2.x, e2.y, q00.x, q00.y);

                pose.pushMatrix();
                // JOML 的 mul 是右乘：this = this * m，即「先 m 后 this」
                pose.set(current).mul(transform);
                graphics.fill(0, 0, 1, 1, color);
                pose.popMatrix();
            }
        }
    }

    /**
     * 三角形的有向面积（屏幕坐标系 y 向下）。
     */
    private static float signedArea(Vector2f p0, Vector2f p1, Vector2f p2) {
        return (p1.x - p0.x) * (p2.y - p0.y) - (p2.x - p0.x) * (p1.y - p0.y);
    }

    /**
     * 四边形的双线性插值：p0=(0,0) p1=(1,0) p2=(1,1) p3=(0,1)。
     */
    private static Vector2f bilinear(Vector2f p0, Vector2f p1, Vector2f p2, Vector2f p3, float u, float v) {
        var topX = p0.x + (p1.x - p0.x) * u;
        var topY = p0.y + (p1.y - p0.y) * u;
        var bottomX = p3.x + (p2.x - p3.x) * u;
        var bottomY = p3.y + (p2.y - p3.y) * u;
        return new Vector2f(topX + (bottomX - topX) * v, topY + (bottomY - topY) * v);
    }

    private static int color(MinecraftVertexBuffer.Vertex vertex) {
        return (vertex.alpha() << 24) | (vertex.red() << 16) | (vertex.green() << 8) | vertex.blue();
    }

    /**
     * 给整个 ARGB 颜色套上 {@code setRsShaderColor} 的着色。
     * <p>
     * GUI 侧的填充/贴图没有顶点色通道（26.2 的 {@code fill} 只吃一个常量颜色），
     * 所以着色只能在这里乘到颜色上。
     */
    private int tint(int argb) {
        if (tint == null) {
            return argb;
        }
        var alpha = Math.min(255, (int) (((argb >>> 24) & 0xFF) * tint[3]));
        var red = Math.min(255, (int) (((argb >> 16) & 0xFF) * tint[0]));
        var green = Math.min(255, (int) (((argb >> 8) & 0xFF) * tint[1]));
        var blue = Math.min(255, (int) ((argb & 0xFF) * tint[2]));
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    private int tint(int value, int channel) {
        if (tint == null) {
            return value;
        }
        return Math.max(0, Math.min(255, (int) (value * tint[channel])));
    }

}
