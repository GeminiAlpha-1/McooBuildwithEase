package com.mcoo.buildwithease.building.structure.builder.standard;

import java.util.List;
import java.util.stream.Stream;

import dev.huskuraft.universal.api.core.Axis;
import dev.huskuraft.universal.api.core.BlockInteraction;
import dev.huskuraft.universal.api.core.BlockPosition;
import dev.huskuraft.universal.api.core.Direction;
import dev.huskuraft.universal.api.core.Player;
import dev.huskuraft.universal.api.math.Vector3d;
import com.mcoo.buildwithease.building.Context;
import com.mcoo.buildwithease.building.structure.BuildMode;
import com.mcoo.buildwithease.building.structure.builder.BlockStructure;

/**
 * 三维斜线。
 * <p>
 * 普通的斜线（{@link DiagonalLine}）两个端点都是「你看着的那个方块」，
 * 所以斜线永远躺在你视线所在的那个平面里 —— 想在空间里拉一根斜的做不到。
 * <p>
 * 这个模式用<strong>三次点击</strong>把斜线撑到三维：
 * <ol>
 *   <li>第 1 次：<strong>起点</strong>（你看着的方块）；</li>
 *   <li>第 2 次：<strong>水平方向</strong>的远端（视线与起点所在水平面的交点）—— 决定往哪走、走多远；</li>
 *   <li>第 3 次：<strong>终点高度</strong>（视线与第 2 点所在竖直线的交点）—— 决定升多高、降多低。</li>
 * </ol>
 * 最终画的是「起点 → 第 3 点」这条真正的空间斜线。
 * <p>
 * 交互方式和 effortless-building-multi 的斜线一致。
 */
public record DiagonalLine3D() implements BlockStructure {

    /**
     * 第 2 个点：视线与「起点所在水平面」的交点 —— 只定水平方向，y 和起点一样。
     * <p>
     * ⚠️ 视线很陡（几乎垂直往下看）时，这个交点会落回**起点方块**里。
     * 框架的交互列表是「一组互不相同的位置」（同位置的会互相替换，见
     * {@code Context.Interactions#put}），重合的点会被去重吃掉 →
     * 点数永远攒不齐 → 建造永远不触发。
     * 所以这里在重合时沿视线的水平方向挪一格，保证它和起点不是同一个方块。
     */
    protected static BlockInteraction traceGround(Player player, Context context) {
        var start = context.getPosition(0);
        var center = start.getCenter();
        var reach = 1024;
        var skipRaytrace = false;

        var result = Stream.of(
                        new BlockStructure.NearestLineCriteria(Axis.Y, player, center, reach, skipRaytrace)
                )
                .filter(BlockStructure.AxisCriteria::isInRange)
                .findAny()
                .map(BlockStructure.AxisCriteria::tracePlane)
                .orElse(null);

        if (result == null) {
            return null;
        }

        var position = result.getBlockPosition();
        if (position.equals(start)) {
            var look = BlockStructure.getEntityLookAngleGap(player);
            position = position.add(look.x() >= 0 ? 1 : -1, 0, look.z() >= 0 ? 1 : -1);
        }

        return result.withBlockPosition(position);
    }

    /**
     * 第 3 个点：终点高度。
     * <p>
     * 取「第 2 点所在的那条竖直线」和视线的交点：竖直线可以用两个竖直面来描述
     * （x = 第 2 点的 x、z = 第 2 点的 z），两个面各算一次，谁更贴近视线就用谁。
     */
    protected static BlockInteraction traceHeight(Player player, Context context) {
        var center = context.getPosition(1).getCenter();
        var reach = 1024;
        var skipRaytrace = false;

        Vector3d best = null;
        var bestDistance = Double.MAX_VALUE;

        for (var axis : List.of(Axis.X, Axis.Z)) {
            var criteria = new BlockStructure.NearestLineCriteria(axis, player, center, reach, skipRaytrace);
            if (!criteria.isInRange()) {
                continue;
            }
            // 视线与这个竖直面的交点
            var plane = criteria.planeVec();
            // 交点的高度配上第 2 点的水平位置 = 竖直线上的那个点
            var projected = new Vector3d(center.x(), plane.y(), center.z());
            var distance = projected.sub(plane).lengthSq();
            if (distance < bestDistance) {
                bestDistance = distance;
                best = projected;
            }
        }

        if (best == null) {
            return null;
        }

        var look = BlockStructure.getEntityLookAngleGap(player);
        return new BlockInteraction(
                player.getEyePosition().add(look.mul(0.001)),
                Direction.getNearest(look.x(), look.y(), look.z()).getOpposite(),
                BlockPosition.at(best),
                true
        );
    }

    public BlockInteraction trace(Player player, Context context, int index) {
        return switch (index) {
            case 0 -> Single.traceSingle(player, context);
            case 1 -> DiagonalLine3D.traceGround(player, context);
            case 2 -> DiagonalLine3D.traceHeight(player, context);
            default -> null;
        };
    }

    public Stream<BlockPosition> collect(Context context, int index) {
        return switch (index) {
            case 1 -> Single.collectSingleBlocks(context);
            // 只点了两次：先按「平面斜线」预览
            case 2 -> DiagonalLine.collectDiagonalLine(List.of(context.getPosition(0), context.getPosition(1)), 0, false);
            // 三点齐了：从起点直接拉到第 3 点（真正的空间斜线）
            case 3 -> DiagonalLine.collectDiagonalLine(List.of(context.getPosition(0), context.getPosition(2)), 0, false);
            default -> Stream.empty();
        };
    }

    @Override
    public int traceSize(Context context) {
        return 3;
    }

    @Override
    public BuildMode getMode() {
        return BuildMode.DIAGONAL_LINE_3D;
    }

}
