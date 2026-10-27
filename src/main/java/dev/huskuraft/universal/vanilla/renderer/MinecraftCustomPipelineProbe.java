package dev.huskuraft.universal.vanilla.renderer;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;

/**
 * 探针：验证 26.2 上能不能**不靠 mixin** 自建渲染管线。
 * <p>
 * 关键点：{@code RenderType.create(String, RenderSetup)} 是包内可见，
 * 但 class tweaker 的 {@code accessible} 能不能把它打开成 public？
 * <p>
 * 这条管线用了 vanilla {@code RenderType} 表达不了的三样东西：
 * <ul>
 *     <li>深度测试 NOTEQUAL（只在「不重叠」处绘制）；</li>
 *     <li>加色混合（{@link BlendFunction#ADDITIVE}）；</li>
 *     <li>不剔除。</li>
 * </ul>
 */
public final class MinecraftCustomPipelineProbe {

    private MinecraftCustomPipelineProbe() {
    }

    /** 描边外壳：NOTEQUAL 深度测试 + 半透明混合 + 不剔除。 */
    public static RenderType outlineShell() {
        var pipeline = RenderPipeline.builder()
                .withLocation("mcoo_build_with_ease:outline_shell")
                .withVertexShader("minecraft:core/position_color")
                .withFragmentShader("minecraft:core/position_color")
                .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
                .withPrimitiveTopology(PrimitiveTopology.QUADS)
                .withDepthStencilState(new DepthStencilState(CompareOp.NOT_EQUAL, false))
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                .withCull(false)
                .build();
        var setup = RenderSetup.builder(pipeline).createRenderSetup();
        return RenderType.create("mcoo_outline_shell", setup);
    }

    /** 永远画在最上层（NEVER 深度测试）+ 加色混合。 */
    public static RenderType overlayAdd() {
        var pipeline = RenderPipeline.builder()
                .withLocation("mcoo_build_with_ease:overlay_add")
                .withVertexShader("minecraft:core/position_color")
                .withFragmentShader("minecraft:core/position_color")
                .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
                .withPrimitiveTopology(PrimitiveTopology.QUADS)
                .withDepthStencilState(new DepthStencilState(CompareOp.NEVER_PASS, false))
                .withColorTargetState(new ColorTargetState(BlendFunction.ADDITIVE))
                .withCull(false)
                .build();
        var setup = RenderSetup.builder(pipeline).createRenderSetup();
        return RenderType.create("mcoo_overlay_add", setup);
    }

    /**
     * 把自建管线登记进 {@code RenderPipelines} 的静态表。
     * <p>
     * {@code ShaderManager} 只扫 {@code RenderPipelines.getStaticPipelines()} 来加载 / 编译着色器，
     * 而那张表就是 {@code PIPELINES_BY_LOCATION}（一个可变 HashMap）——
     * 登记进去，着色器才会被加载。
     */
    public static void register(RenderPipeline pipeline) {
        RenderPipelines.PIPELINES_BY_LOCATION.put(pipeline.getLocation(), pipeline);
    }

}
