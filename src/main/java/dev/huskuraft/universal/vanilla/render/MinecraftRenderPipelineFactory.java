package dev.huskuraft.universal.vanilla.render;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

import dev.huskuraft.universal.api.core.ResourceLocation;
import dev.huskuraft.universal.api.render.RenderPipelineSpec;

/**
 * 把 {@link RenderPipelineSpec}（MC 无关的语义描述）翻译成 26.2 的 {@link RenderType}。
 * <p>
 * <b>这是「跟版成本」真正集中的地方</b>：业务层的模块只描述语义
 * （深度测试 / 混合 / 剔除 / 用哪套着色器），具体怎么落到某个 Minecraft 版本的
 * 管线体系上，全在这个文件里。换 MC 版本只需要改这里。
 * <p>
 * 管线是自建的（{@code RenderPipeline.builder} + {@code RenderPipelines#register}），
 * 所以能表达 vanilla {@code RenderType} 表达不了的深度测试与混合语义；
 * 但着色器先复用 vanilla 现成的那几套，所以第一版不需要自研 GLSL。
 */
public final class MinecraftRenderPipelineFactory {

    /** 命名空间 —— 自建管线都挂在它下面，调试日志靠它识别。 */
    public static final String NAMESPACE = "mcoo_build_with_ease";

    private static final Map<String, RenderType> CACHE = new ConcurrentHashMap<>();

    private MinecraftRenderPipelineFactory() {
    }

    /** 拿（或建）一条管线。同一个 spec.id 只会真正创建一次。 */
    public static RenderType create(RenderPipelineSpec spec, ResourceLocation texture) {
        return CACHE.computeIfAbsent(spec.id(), id -> {
            var pipeline = buildPipeline(spec);
            var builder = RenderSetup.builder(pipeline);
            if (spec.shaderSet() == RenderPipelineSpec.ShaderSet.ENTITY
                    || spec.shaderSet() == RenderPipelineSpec.ShaderSet.BLOCK) {
                // 实体 / 方块管线要采样贴图；没给贴图时绑一张 1x1 白图（和 vanilla 一样是必需项）
                builder.withTexture("Sampler0", texture != null ? toIdentifier(texture) : Identifier.withDefaultNamespace("textures/block/white.png"));
            }
            if (spec.lightmap()) {
                builder.useLightmap();
            }
            if (spec.overlay()) {
                builder.useOverlay();
            }
            return RenderType.create(spec.id(), builder.createRenderSetup());
        });
    }

    private static RenderPipeline buildPipeline(RenderPipelineSpec spec) {
        var builder = RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
                .withLocation(Identifier.fromNamespaceAndPath(NAMESPACE, spec.id()))
                .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
                .withDepthStencilState(new DepthStencilState(compareOp(spec.depthTest()), spec.writeDepth()))
                .withCull(spec.cull());

        var blend = blendFunction(spec.blend());
        if (blend != null) {
            builder.withColorTargetState(new ColorTargetState(blend));
        }

        // 顶点格式跟着着色器套件走
        switch (spec.shaderSet()) {
            case ENTITY -> builder.withVertexBinding(0, DefaultVertexFormat.ENTITY);
            case POSITION_COLOR -> builder.withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR);
            case BLOCK -> builder.withVertexBinding(0, DefaultVertexFormat.BLOCK);
            case LINES -> builder.withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR_NORMAL);
            case SCREEN_QUAD -> builder.withVertexBinding(0, DefaultVertexFormat.POSITION);
        }

        return RenderPipelines.register(builder.build());
    }

    private static CompareOp compareOp(RenderPipelineSpec.DepthTest depthTest) {
        return switch (depthTest) {
            case ALWAYS -> CompareOp.ALWAYS_PASS;
            case LESS -> CompareOp.LESS_THAN;
            case LESS_OR_EQUAL -> CompareOp.LESS_THAN_OR_EQUAL;
            case EQUAL -> CompareOp.EQUAL;
            case NOT_EQUAL -> CompareOp.NOT_EQUAL;
            case GREATER_OR_EQUAL -> CompareOp.GREATER_THAN_OR_EQUAL;
            case GREATER -> CompareOp.GREATER_THAN;
            case NEVER -> CompareOp.NEVER_PASS;
        };
    }

    private static BlendFunction blendFunction(RenderPipelineSpec.BlendMode blend) {
        return switch (blend) {
            case OPAQUE -> null;
            case TRANSLUCENT -> BlendFunction.TRANSLUCENT;
            case ADDITIVE -> BlendFunction.ADDITIVE;
        };
    }

    private static Identifier toIdentifier(ResourceLocation location) {
        if (location == null) {
            return null;
        }
        return Identifier.fromNamespaceAndPath(location.getNamespace(), location.getPath());
    }

}
