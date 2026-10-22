package dev.huskuraft.universal.vanilla.renderer;

import dev.huskuraft.universal.api.core.ResourceLocation;
import dev.huskuraft.universal.api.renderer.Shader;
import dev.huskuraft.universal.api.renderer.Uniform;
import dev.huskuraft.universal.api.renderer.VertexFormat;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

/**
 * 26.2 删掉了 {@code ShaderInstance} / {@code CoreShaders}，着色器不再是可以被逐个「拿到并设 uniform」
 * 的对象，而是被 {@link com.mojang.blaze3d.pipeline.RenderPipeline} 引用的
 * {@code Identifier}（顶点/片元着色器路径）。
 * <p>
 * 所以这里用 {@link com.mojang.blaze3d.pipeline.RenderPipeline} 作为「着色器」的载体：
 * {@link #getResource()} 返回管线位置，{@link #getVertexFormat()} 返回管线绑定的顶点格式。
 */
public record MinecraftShader(
        com.mojang.blaze3d.pipeline.RenderPipeline refs
) implements Shader {

    /** common-api 的 {@link ResourceLocation} 适配器（26.2 的 {@link Identifier}）。 */
    public record PipelineLocation(Identifier refs) implements ResourceLocation {

        @Override
        public String getNamespace() {
            return refs.getNamespace();
        }

        @Override
        public String getPath() {
            return refs.getPath();
        }

        @Override
        public String toString() {
            return getNamespace() + ":" + getPath();
        }
    }

    @Override
    public ResourceLocation getResource() {
        if (refs == null) {
            return null;
        }
        return new PipelineLocation(refs.getLocation());
    }

    public VertexFormat getVertexFormat() {
        if (refs == null) {
            return null;
        }
        var bindings = refs.getVertexFormatBindings();
        if (bindings == null || bindings.length == 0 || bindings[0] == null) {
            return null;
        }
        var format = bindings[0];
        return () -> format;
    }

    /**
     * 26.2 的 uniform 由管线自己管理（{@code BindGroupLayout} / {@code UniformValue}），
     * 没有对外暴露的可写 uniform 对象，这里只能给一个空实现。
     */
    public Uniform getUniform(String param) {
        return new MinecraftUniform();
    }

    /** 便捷方法：取管线绑定的顶点格式。 */
    public com.mojang.blaze3d.vertex.VertexFormat platformVertexFormat() {
        if (refs == null) {
            return null;
        }
        var bindings = refs.getVertexFormatBindings();
        if (bindings == null || bindings.length == 0) {
            return null;
        }
        return bindings[0];
    }

    /** 便捷方法：取管线对应的 {@link RenderType} 风格描述（仅用于日志/调试）。 */
    public String pipelineLocation() {
        return refs == null ? null : refs.getLocation().toString();
    }

}
