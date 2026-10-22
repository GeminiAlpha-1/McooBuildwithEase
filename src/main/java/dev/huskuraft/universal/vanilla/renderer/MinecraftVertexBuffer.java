package dev.huskuraft.universal.vanilla.renderer;

import java.util.List;

import dev.huskuraft.universal.api.renderer.VertexBuffer;

/**
 * 顶点缓冲。
 * <p>
 * 26.2 删掉了 {@code MultiBufferSource.BufferSource}，顶点不能像 1.21.3 那样「拿到就写」：
 * 真正的写入发生在 {@code SubmitNodeCollector#submitCustomGeometry} 回调里拿到的
 * {@code VertexConsumer} 上，而且那是<b>延迟</b>执行的。
 * <p>
 * 为了保住 common-api 的立即模式写法（{@code bufferSource().getBuffer(layer)} 写顶点 →
 * {@code flush()}），这里先把顶点<b>记录</b>下来，再由 {@link MinecraftBufferSource} 在提交时
 * 重放进 26.2 的 {@code VertexConsumer}。
 */
public class MinecraftVertexBuffer implements VertexBuffer {

    /** 顶点用到的属性位。 */
    public static final int ATTRIBUTE_COLOR = 1;
    public static final int ATTRIBUTE_UV = 1 << 1;
    public static final int ATTRIBUTE_UV1 = 1 << 2;
    public static final int ATTRIBUTE_UV2 = 1 << 3;
    public static final int ATTRIBUTE_NORMAL = 1 << 4;

    /** 一个顶点的全部数据。 */
    public record Vertex(
            float x, float y, float z,
            int red, int green, int blue, int alpha,
            float u, float v,
            int u1, int v1,
            int u2, int v2,
            float nx, float ny, float nz,
            int attributes
    ) {
    }

    private final List<Vertex> sink;

    private float x;
    private float y;
    private float z;
    private int red = 255;
    private int green = 255;
    private int blue = 255;
    private int alpha = 255;
    private float u;
    private float v;
    private int u1;
    private int v1;
    private int u2;
    private int v2;
    private float nx;
    private float ny;
    private float nz;
    private int attributes;

    public MinecraftVertexBuffer(List<Vertex> sink) {
        this.sink = sink;
    }

    @Override
    public Object refs() {
        return sink;
    }

    @Override
    public MinecraftVertexBuffer vertex(double x, double y, double z) {
        this.x = (float) x;
        this.y = (float) y;
        this.z = (float) z;
        return this;
    }

    @Override
    public MinecraftVertexBuffer color(int red, int green, int blue, int alpha) {
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;
        this.attributes |= ATTRIBUTE_COLOR;
        return this;
    }

    @Override
    public MinecraftVertexBuffer uv(float u, float v) {
        this.u = u;
        this.v = v;
        this.attributes |= ATTRIBUTE_UV;
        return this;
    }

    @Override
    public MinecraftVertexBuffer uv1(int u, int v) {
        this.u1 = u;
        this.v1 = v;
        this.attributes |= ATTRIBUTE_UV1;
        return this;
    }

    @Override
    public MinecraftVertexBuffer uv2(int u, int v) {
        this.u2 = u;
        this.v2 = v;
        this.attributes |= ATTRIBUTE_UV2;
        return this;
    }

    @Override
    public MinecraftVertexBuffer normal(float x, float y, float z) {
        this.nx = x;
        this.ny = y;
        this.nz = z;
        this.attributes |= ATTRIBUTE_NORMAL;
        return this;
    }

    @Override
    public void endVertex() {
        sink.add(new Vertex(x, y, z, red, green, blue, alpha, u, v, u1, v1, u2, v2, nx, ny, nz, attributes));
        x = 0;
        y = 0;
        z = 0;
        red = 255;
        green = 255;
        blue = 255;
        alpha = 255;
        u = 0;
        v = 0;
        u1 = 0;
        v1 = 0;
        u2 = 0;
        v2 = 0;
        nx = 0;
        ny = 0;
        nz = 0;
        attributes = 0;
    }

}
