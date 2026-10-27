package dev.huskuraft.universal.vanilla.renderer;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * 本模组自建的渲染管线。
 * <p>
 * <b>为什么能自建</b>：26.2 的 {@code RenderPipeline} 是可编程的描述（后端无关，
 * OpenGL / Vulkan 同一份），{@code RenderPipeline.builder} 是 public；
 * 唯一卡点 {@code RenderType.create} 是包内可见、{@code RenderPipelines#register} 是 private
 * —— 两者都用 class tweaker 打开（见 {@code mcoo_build_with_ease.classtweaker}），
 * <b>所以整套自建管线不需要 mixin</b>。
 * <p>
 * <b>怎么照抄 vanilla 的配方</b>：用 vanilla 的 {@code RenderPipelines#ENTITY_SNIPPET} 建管线，
 * 它的 bind group / 雾 / 光照 / 顶点格式就和 vanilla 的实体管线完全一致；
 * 只把深度测试、混合、剔除换成我们自己的。
 * <p>
 * <b>为什么必须登记</b>：{@code ShaderManager} 只扫 {@code RenderPipelines.getStaticPipelines()}
 * 这一张表来加载 / 编译着色器，不登记的话自建管线的着色器根本不会被加载。
 * <p>
 * 着色器本身直接复用 vanilla 的 {@code minecraft:core/entity}（{@code entity.vsh} + {@code entity.fsh}），
 * 所以第一版连自研 GLSL 都不用写。
 */
public final class MinecraftCustomPipelines {

    /** 描边外壳：NOT_EQUAL 深度测试（只在没有重叠的地方绘制）。 */
    public static final RenderPipeline OUTLINE_SHELL = build(
            "outline_shell",
            new DepthStencilState(CompareOp.NOT_EQUAL, false),
            new ColorTargetState(BlendFunction.TRANSLUCENT),
            false);

    /** 描边覆盖层：NEVER 深度测试（永远画在最上层）。 */
    public static final RenderPipeline OUTLINE_OVERLAY = build(
            "outline_overlay",
            new DepthStencilState(CompareOp.NEVER_PASS, false),
            new ColorTargetState(BlendFunction.TRANSLUCENT),
            false);

    /** 加色混合层。 */
    public static final RenderPipeline ADDITIVE = build(
            "additive",
            new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false),
            new ColorTargetState(BlendFunction.ADDITIVE),
            false);

    private MinecraftCustomPipelines() {
    }

    /**
     * 触发本类的静态初始化（把自建管线登记进 vanilla 的管线表）。
     * <p>
     * <b>必须在客户端第一次资源重载之前调用</b> ——
     * {@code ShaderManager} 只在资源重载时扫描 {@code RenderPipelines.getStaticPipelines()}，
     * 登记晚了这一轮就白登记了（着色器不会被加载）。
     */
    public static void ensureRegistered() {
        // 引用一个非编译期常量的静态字段即可触发类初始化
        assert OUTLINE_SHELL != null;
    }

    private static RenderPipeline build(String name, DepthStencilState depth, ColorTargetState color, boolean cull) {
        var pipeline = RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
                .withLocation(Identifier.fromNamespaceAndPath("mcoo_build_with_ease", name))
                .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
                .withDepthStencilState(depth)
                .withColorTargetState(color)
                .withCull(cull)
                .build();
        // 登记进 vanilla 的管线表，ShaderManager 才会加载它的着色器
        return RenderPipelines.register(pipeline);
    }

}
