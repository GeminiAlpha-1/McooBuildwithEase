package dev.huskuraft.universal.api.renderer;

/**
 * vanilla 现成的渲染层。
 * <p>
 * <b>为什么需要显式枚举</b>：26.2 删掉了 {@code RenderStateShard} / {@code MultiBufferSource}，
 * 渲染状态变成「后端无关的 {@code RenderPipeline}」+「{@code RenderSetup}」+「{@code RenderType}」。
 * 而 {@code RenderType.create(String, RenderSetup)} 是<b>包内可见</b> —— 第三方 mod 无法自建
 * {@code RenderType}，只能复用 {@code RenderTypes} 里的现成条目。
 * <p>
 * 所以「这条渲染层用哪条 vanilla 管线」必须显式写出来，由平台层翻译成具体的 {@code RenderType}。
 * 这比「按顶点格式去猜」要可靠得多：
 * <ul>
 *     <li>猜出来的行为会随顶点格式改动而漂移；</li>
 *     <li>26.2 的 {@code RenderType} 语义（深度测试、剔除、混合、输出目标）都是<b>固定值</b>，
 *         而上游 1.21.3 的 {@code RenderStateShard} 是可自由组合的 —— 猜出来的结果经常和
 *         上游的意图对不上（例如「只在不重叠处绘制」这种深度测试语义直接就丢了）。</li>
 * </ul>
 * <p>
 * 需要注意的是：vanilla 管线是<b>共享</b>的。装了 Iris / Sodium 这类接管渲染管线的 mod 时，
 * 它们映射的是 vanilla 的地形 / 实体 / 线框程序，所以走 {@code RenderType} 的绘制会被一并接管；
 * 而 mod 自建管线（{@code RenderPipeline}）不在它们的映射表里，观感会与 vanilla 下不同。
 */
public enum VanillaLayer {

    /** 线框（不透明）。 */
    LINES,

    /** 线框（半透明）。 */
    LINES_TRANSLUCENT,

    /** 纯色填充（顶点色，无贴图）。GUI 的矩形 / 四边形走这条。 */
    DEBUG_FILLED_BOX,

    /** 不透明方块（方块图集）。 */
    SOLID_BLOCK,

    /** 半透明方块（方块图集）。 */
    TRANSLUCENT_BLOCK,

    /** 不透明实体（需要贴图）。 */
    ENTITY_SOLID,

    /** 半透明实体（需要贴图）。 */
    ENTITY_TRANSLUCENT,

    /** 文字（需要贴图）。GUI 的带贴图矩形走这条。 */
    TEXT,

}
