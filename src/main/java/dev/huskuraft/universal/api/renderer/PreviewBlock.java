package dev.huskuraft.universal.api.renderer;

import dev.huskuraft.universal.api.core.BlockPosition;
import dev.huskuraft.universal.api.core.BlockState;

/**
 * 模型预览里的一块方块。
 *
 * @param position 方块坐标（整格）
 * @param state    方块状态（决定用哪个模型和贴图）
 */
public record PreviewBlock(BlockPosition position, BlockState state) {
}
