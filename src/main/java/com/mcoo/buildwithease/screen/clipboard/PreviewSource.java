package com.mcoo.buildwithease.screen.clipboard;

import java.util.List;

import com.mcoo.buildwithease.building.clipboard.Snapshot;
import dev.huskuraft.universal.api.renderer.PreviewBlock;

/**
 * 把剪贴板快照变成「可预览的方块列表」—— 结构预览的<b>唯一数据入口</b>。
 *
 * <p>自研 GUI 的预览控件和 ImGui 的剪贴板面板都从这里取数据，
 * 保证两边看到的是同一个结构、同一套过滤规则（没有方块状态的条目要跳过，
 * 否则平台层拿到 null 会直接崩）。
 */
public final class PreviewSource {

    private PreviewSource() {
    }

    /** 要预览的方块（跳过没有方块状态的条目）。 */
    public static List<PreviewBlock> blocksOf(Snapshot snapshot) {
        if (snapshot == null || snapshot.blockData() == null) {
            return List.of();
        }
        return snapshot.blockData().stream()
                .filter(blockData -> blockData.blockState() != null)
                .map(blockData -> new PreviewBlock(
                        blockData.blockPosition(), blockData.blockState(), blockData.entityTag()))
                .toList();
    }

    /**
     * 结构的指纹：方块数 + 各方块坐标与状态的哈希。
     * <p>
     * 用来判断「结构有没有变」—— 变了才重建方块列表与网格。
     * 坐标和状态都参与哈希，所以挪动一格或换一种方块都会触发重建。
     */
    public static int fingerprintOf(Snapshot snapshot) {
        if (snapshot == null || snapshot.blockData() == null) {
            return 0;
        }
        var hash = 17;
        for (var blockData : snapshot.blockData()) {
            var position = blockData.blockPosition();
            hash = hash * 31 + position.x();
            hash = hash * 31 + position.y();
            hash = hash * 31 + position.z();
            hash = hash * 31 + (blockData.blockState() == null ? 0 : blockData.blockState().hashCode());
        }
        return hash;
    }

}
