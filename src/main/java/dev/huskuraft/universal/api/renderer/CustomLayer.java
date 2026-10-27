package dev.huskuraft.universal.api.renderer;

/**
 * 本模组<b>自建</b>的渲染层。
 * <p>
 * 26.2 的 vanilla {@code RenderType} 把深度测试、剔除、混合、输出目标都固定死了 ——
 * 上游 1.21.3 用 {@code RenderStateShard} 自由组合出来的那几种语义（只在不重叠处绘制、
 * 永远画在最上层、加色混合）在 vanilla 里**没有对应的现成条目**。
 * <p>
 * 但 26.2 的 {@code RenderPipeline} 是可以自己建的（{@code RenderPipeline.builder} 是 public），
 * 描述性、后端无关 → 同一份定义同时喂给 OpenGL 和 Vulkan。
 * 所以这几层由本模组自建，语义与上游 1.21.3 一一对应。
 */
public enum CustomLayer {

    /**
     * 描边外壳：深度测试 {@code NOT_EQUAL}。
     * <p>
     * 只在「没有和已有几何重叠」的地方绘制 —— 上游用来画选区边框，
     * 让被其它方块挡住的部分不显示。
     */
    OUTLINE_SHELL,

    /**
     * 描边覆盖层：深度测试 {@code NEVER_PASS}。
     * <p>
     * 无视深度，永远画在最上层 —— 上游用来画「不管有没有被挡住都要看见」的边框。
     */
    OUTLINE_OVERLAY,

    /** 加色混合层：{@code BlendFunction.ADDITIVE}，用于发光面。 */
    ADDITIVE,

}
