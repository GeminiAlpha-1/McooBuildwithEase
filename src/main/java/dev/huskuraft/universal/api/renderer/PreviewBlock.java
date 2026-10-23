package dev.huskuraft.universal.api.renderer;

import dev.huskuraft.universal.api.core.BlockPosition;
import dev.huskuraft.universal.api.core.BlockState;
import dev.huskuraft.universal.api.tag.RecordTag;

/**
 * 模型预览里的一块方块。
 *
 * @param position 方块坐标（整格）
 * @param state    方块状态（决定用哪个模型和贴图）
 * @param entityTag 方块实体的 NBT；为 null 表示这个方块没有方块实体。
 *                 <p>
 *                 方块实体（钟、箱子、陶罐、旗帜…）的形状<b>不在</b>方块状态模型里，
 *                 而是由各自的方块实体渲染器在 3D 管线里画 ——
 *                 所以要把 NBT 一起带过去，渲染器才能还原出正确的朝向和内容。
 */
public record PreviewBlock(BlockPosition position, BlockState state, RecordTag entityTag) {

    public PreviewBlock(BlockPosition position, BlockState state) {
        this(position, state, null);
    }
}
