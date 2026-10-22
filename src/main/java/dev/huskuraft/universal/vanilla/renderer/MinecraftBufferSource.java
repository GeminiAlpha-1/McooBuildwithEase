package dev.huskuraft.universal.vanilla.renderer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import dev.huskuraft.universal.api.renderer.BufferSource;
import dev.huskuraft.universal.api.renderer.RenderLayer;
import dev.huskuraft.universal.api.renderer.VertexBuffer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

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
    private final Map<RenderType, Batch> batches = new LinkedHashMap<>();

    public MinecraftBufferSource(SubmitNodeCollector collector, PoseStack poseStack, float[] tint) {
        this.collector = collector;
        this.poseStack = poseStack;
        this.graphics = null;
        this.tint = tint;
    }

    public MinecraftBufferSource(GuiGraphicsExtractor graphics, float[] tint) {
        this.collector = null;
        this.poseStack = null;
        this.graphics = graphics;
        this.tint = tint;
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
                graphics.blit(batch.texture(), x1, y1, x2, y2, u0, v0, u1, v1);
                continue;
            }

            var colorA = color(a);
            var colorB = color(b);
            var colorC = color(c);
            var colorD = color(d);
            if (colorA == colorB && colorB == colorC && colorC == colorD) {
                graphics.fill(x1, y1, x2, y2, colorA);
            } else {
                graphics.fillGradient(x1, y1, x2, y2, colorA, colorB);
            }
        }
    }

    private static int color(MinecraftVertexBuffer.Vertex vertex) {
        return (vertex.alpha() << 24) | (vertex.red() << 16) | (vertex.green() << 8) | vertex.blue();
    }

    private int tint(int value, int channel) {
        if (tint == null) {
            return value;
        }
        return Math.max(0, Math.min(255, (int) (value * tint[channel])));
    }

}
