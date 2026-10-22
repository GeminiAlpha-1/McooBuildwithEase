package com.mcoo.buildwithease.screen.clipboard;

import java.util.ArrayList;
import java.util.Comparator;

import dev.huskuraft.universal.api.gui.AbstractWidget;
import dev.huskuraft.universal.api.platform.Entrance;
import dev.huskuraft.universal.api.renderer.Renderer;
import dev.huskuraft.universal.api.text.Text;
import com.mcoo.buildwithease.building.clipboard.Snapshot;

public final class StructureSnapshotWidget extends AbstractWidget {

    /**
     * 一个方块在预览里占多少像素。
     * <p>
     * GUI 里的方块物品模型是 16×16 格里画一个 0.625 缩放的立方体 → 实际约 10 像素。
     */
    private static final float BLOCK_PIXELS = 10.0F;

    private Snapshot snapshot;

    public StructureSnapshotWidget(Entrance entrance, int x, int y, int width, int height, Snapshot snapshot) {
        super(entrance, x, y, width, height, Text.empty());
        this.snapshot = snapshot;
    }

    @Override
    public void onCreate() {
    }

    /**
     * 剪贴板的结构预览。
     * <p>
     * 【26.2 的限制】GUI 只有 2D 渲染管线（1.21.3 时 GUI 用的是 3D PoseStack，可以直接摆方块模型），
     * 所以这里改成「等轴测投影 + 每格画一个方块物品」：
     * 方块的物品模型本身就是 3D 立方体，视觉上和上游的 3D 预览接近。
     * <p>
     * 投影朝向和 26.2 物品模型（{@code ItemDisplayContext.GUI}）保持一致 ——
     * 先绕 Y 轴 225°、再绕 X 轴 30°，这样方块自身的朝向和摆放方向不会互相打架。
     */
    @Override
    public void renderWidget(Renderer renderer, int mouseX, int mouseY, float deltaTick) {
        if (snapshot == null || snapshot.isEmpty()) {
            return;
        }
        var blocks = snapshot.blockData().stream()
                .filter(blockData -> blockData.blockState() != null)
                .toList();
        if (blocks.isEmpty()) {
            return;
        }

        final var cosY = Math.cos(Math.toRadians(225));
        final var sinY = Math.sin(Math.toRadians(225));
        final var cosX = Math.cos(Math.toRadians(30));
        final var sinX = Math.sin(Math.toRadians(30));

        var center = snapshot.getCenter();
        var projected = new ArrayList<double[]>(blocks.size());
        for (var blockData : blocks) {
            var pos = blockData.blockPosition().getCenter();
            var rx = pos.x() - center.x();
            var ry = pos.y() - center.y();
            var rz = pos.z() - center.z();
            // 绕 Y 轴 225°
            var x1 = rx * cosY + rz * sinY;
            var z1 = -rx * sinY + rz * cosY;
            // 绕 X 轴 30°
            var y2 = ry * cosX - z1 * sinX;
            var z2 = ry * sinX + z1 * cosX;
            // 屏幕坐标（y 向下）
            projected.add(new double[]{x1, -y2, z2});
        }

        var minX = projected.stream().mapToDouble(p -> p[0]).min().orElse(0);
        var maxX = projected.stream().mapToDouble(p -> p[0]).max().orElse(0);
        var minY = projected.stream().mapToDouble(p -> p[1]).min().orElse(0);
        var maxY = projected.stream().mapToDouble(p -> p[1]).max().orElse(0);
        var centerX = (minX + maxX) / 2;
        var centerY = (minY + maxY) / 2;

        var contentWidth = (float) Math.max(1, maxX - minX + 1) * BLOCK_PIXELS;
        var contentHeight = (float) Math.max(1, maxY - minY + 1) * BLOCK_PIXELS;
        var scale = Math.min(getWidth() / contentWidth, getHeight() / contentHeight) * 0.9F;
        scale = Math.max(scale, 0.05F);

        var originX = getX() + getWidth() / 2F;
        var originY = getY() + getHeight() / 2F;

        // 远的先画、近的后画（画家算法）
        var order = new ArrayList<Integer>(blocks.size());
        for (var i = 0; i < blocks.size(); i++) {
            order.add(i);
        }
        order.sort(Comparator.comparingDouble(i -> projected.get(i)[2]));

        renderer.pushScissor(getLeft(), getTop(), getWidth(), getHeight());
        for (var i : order) {
            var blockData = blocks.get(i);
            var point = projected.get(i);
            var x = originX + (float) ((point[0] - centerX) * BLOCK_PIXELS * scale);
            var y = originY + (float) ((point[1] - centerY) * BLOCK_PIXELS * scale);
            var stack = blockData.blockState().getItem().getDefaultStack();
            if (stack == null) {
                continue;
            }
            renderer.renderItemScaled(stack, (int) (x - BLOCK_PIXELS * scale / 2F), (int) (y - BLOCK_PIXELS * scale / 2F), scale);
        }
        renderer.popScissor();
    }

    private int backgroundColor = 0xdc000000;


    @Override
    public void renderWidgetBackground(Renderer renderer, int mouseX, int mouseY, float deltaTick) {
        renderer.renderGradientRect(getLeft(), getTop(), getRight(), getBottom(), backgroundColor, backgroundColor);
    }

    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public void setSnapshot(Snapshot snapshot) {
        this.snapshot = snapshot;
    }
}
