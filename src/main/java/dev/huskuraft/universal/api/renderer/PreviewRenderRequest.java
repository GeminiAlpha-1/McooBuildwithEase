package dev.huskuraft.universal.api.renderer;

import java.util.List;

/**
 * 三维结构预览的渲染请求。
 * <p>
 * 由业务层描述「要看什么」，由平台层决定「怎么画」。
 * 平台层负责把方块模型与方块实体（钟、箱子、陶罐、旗帜等）一起画出来 ——
 * 方块实体的形状不在方块状态模型里，只有平台层才知道怎么渲染。
 *
 * @param blocks      要预览的方块（位置 + 状态）
 * @param camera      相机（偏航 / 俯仰 / 缩放 / 平移）
 * @param orientation 朝向修正（绕世界轴各转多少个 90°）；没有就用 {@link PreviewOrientation#DEFAULT}
 */
public record PreviewRenderRequest(
        List<PreviewBlock> blocks,
        PreviewCamera camera,
        PreviewOrientation orientation
) {

    public PreviewRenderRequest(List<PreviewBlock> blocks, PreviewCamera camera) {
        this(blocks, camera, PreviewOrientation.DEFAULT);
    }
}
