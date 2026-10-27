package dev.huskuraft.universal.api.render;

import java.util.List;

/**
 * 一条渲染管线的<b>语义描述</b>（与 Minecraft 无关）。
 * <p>
 * 业务层只说「我要一条什么样的管线」（深度测试、混合、剔除、用哪套着色器），
 * 由平台层翻译成具体版本的 {@code RenderPipeline} / {@code RenderType}。
 * 这样新增一版 Minecraft 只需要改平台层，模块代码一行都不用动。
 *
 * @param id          管线标识（模块内唯一）
 * @param shaderSet   用哪套着色器（决定顶点格式与光照输入）
 * @param depthTest   深度比较方式
 * @param writeDepth  是否写深度
 * @param blend       混合方式
 * @param cull        是否背面剔除
 * @param lightmap    是否采样光照图
 * @param overlay     是否采样 overlay（物品破坏进度等）
 */
public record RenderPipelineSpec(
        String id,
        ShaderSet shaderSet,
        DepthTest depthTest,
        boolean writeDepth,
        BlendMode blend,
        boolean cull,
        boolean lightmap,
        boolean overlay
) {

    /** 着色器套件 —— 决定顶点格式和着色器输入。 */
    public enum ShaderSet {
        /** 实体管线：位置/颜色/法线/贴图/光照图。 */
        ENTITY,
        /** 纯色：位置 + 颜色。 */
        POSITION_COLOR,
        /** 方块：位置/颜色/贴图/光照图。 */
        BLOCK,
        /** 线框。 */
        LINES,
        /** 全屏四边形。 */
        SCREEN_QUAD,
    }

    /** 深度比较。 */
    public enum DepthTest {
        /** 永远通过。 */
        ALWAYS,
        /** 小于。 */
        LESS,
        /** 小于等于。 */
        LESS_OR_EQUAL,
        /** 等于。 */
        EQUAL,
        /** 不等于。 */
        NOT_EQUAL,
        /** 大于等于。 */
        GREATER_OR_EQUAL,
        /** 大于。 */
        GREATER,
        /** 永远不通过。 */
        NEVER,
    }

    /** 混合方式。 */
    public enum BlendMode {
        /** 不透明。 */
        OPAQUE,
        /** 普通半透明。 */
        TRANSLUCENT,
        /** 加色。 */
        ADDITIVE,
    }

    public static RenderPipelineSpec of(String id, ShaderSet shaderSet, DepthTest depthTest, BlendMode blend) {
        return new RenderPipelineSpec(id, shaderSet, depthTest, false, blend, true, true, true);
    }

    public RenderPipelineSpec withCull(boolean newCull) {
        return new RenderPipelineSpec(id, shaderSet, depthTest, writeDepth, blend, newCull, lightmap, overlay);
    }

    public RenderPipelineSpec withWriteDepth(boolean newWriteDepth) {
        return new RenderPipelineSpec(id, shaderSet, depthTest, newWriteDepth, blend, cull, lightmap, overlay);
    }

    public RenderPipelineSpec withLightmap(boolean newLightmap) {
        return new RenderPipelineSpec(id, shaderSet, depthTest, writeDepth, blend, cull, newLightmap, overlay);
    }

    public RenderPipelineSpec withOverlay(boolean newOverlay) {
        return new RenderPipelineSpec(id, shaderSet, depthTest, writeDepth, blend, cull, lightmap, newOverlay);
    }

    /** 常用组合：半透明、不写深度、不剔除的描边层。 */
    public static List<RenderPipelineSpec> outlineDefaults(String prefix) {
        return List.of(
                RenderPipelineSpec.of(prefix + "_shell", ShaderSet.ENTITY, DepthTest.NOT_EQUAL, BlendMode.TRANSLUCENT)
                        .withCull(false),
                RenderPipelineSpec.of(prefix + "_overlay", ShaderSet.ENTITY, DepthTest.NEVER, BlendMode.TRANSLUCENT)
                        .withCull(false));
    }

}
