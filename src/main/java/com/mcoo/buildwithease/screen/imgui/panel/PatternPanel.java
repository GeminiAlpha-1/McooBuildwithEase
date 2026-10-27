package com.mcoo.buildwithease.screen.imgui.panel;

import com.mcoo.buildwithease.McooBuildwithEaseClient;
import com.mcoo.buildwithease.building.pattern.Pattern;
import com.mcoo.buildwithease.building.pattern.Transformer;
import com.mcoo.buildwithease.building.pattern.array.ArrayTransformer;
import com.mcoo.buildwithease.building.pattern.mirror.MirrorTransformer;
import com.mcoo.buildwithease.building.pattern.raidal.RadialTransformer;
import com.mcoo.buildwithease.building.pattern.randomize.ItemRandomizer;
import com.mcoo.buildwithease.screen.imgui.widget.ImGuiShell;
import com.mcoo.buildwithease.screen.imgui.widget.ImGuiWidgets;

import java.util.List;

/**
 * 图案面板：<b>中间</b>是当前图案的变换器栈，<b>右侧</b>是选中那一项的详情。
 *
 * <h3>图案是什么</h3>
 * 「图案」= 一串 {@link Transformer}（阵列 / 镜像 / 环形 / 物品随机化），
 * 建造时按顺序作用在每一次操作上 —— 所以它本质是一个<b>可叠加的效果栈</b>，
 * 这正好契合「左列表 + 右详情」的编辑器布局。
 *
 * <p>这一版是<b>只读浏览 + 总开关</b>：能看清栈里有什么、每项的参数摘要。
 * 改参数（阵列的偏移/次数、镜像的轴、环形的角度…）每种类型表单都不一样，
 * 属于下一版的事 —— 先把结构和观感定下来，别在表单细节上返工。
 */
public final class PatternPanel implements ImGuiPanel {

    private static final String ID = "pattern";

    private final McooBuildwithEaseClient entrance;

    /** 当前选中的变换器 id（null = 默认选第一个）。 */
    private java.util.UUID selectedId;

    public PatternPanel(McooBuildwithEaseClient entrance) {
        this.entrance = entrance;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String titleKey() {
        return "mcoo_build_with_ease.pattern.title";
    }

    // ------------------------------------------------------------------
    // 中间栏：总开关 + 变换器栈
    // ------------------------------------------------------------------

    @Override
    public void renderContent() {
        var pattern = pattern();
        if (pattern == null) {
            ImGuiWidgets.diagnostic("(no pattern in this session)");
            return;
        }

        // 总开关：图案关掉时建造不套任何变换（和自研界面语义一致）
        boolean enabled = pattern.enabled();
        if (ImGuiWidgets.checkbox("mcoo_build_with_ease.pattern.button.enable", enabled)) {
            setPattern(pattern.withEnabled(!enabled));
            return;
        }

        ImGuiWidgets.separator();

        var transformers = pattern.transformers();
        if (transformers.isEmpty()) {
            ImGuiWidgets.text("mcoo_build_with_ease.pattern.empty");
            return;
        }
        if (selectedId == null) {
            selectedId = transformers.get(0).getId();
        }

        for (var transformer : transformers) {
            boolean selected = transformer.getId().equals(selectedId);
            // 行首用 ✓/✗ 表示这一项当前是否有效（参数不完整时 isValid() 会是 false）
            String mark = transformer.isValid() ? "● " : "○ ";
            String label = mark
                    + transformer.getType().getDisplayName().getString()
                    + "  " + transformer.name().getString()
                    + "  ×" + String.format("%.2f", transformer.volumeMultiplier());
            if (ImGuiShell.navItemRaw(label, selected)) {
                selectedId = transformer.getId();
            }
        }
    }

    // ------------------------------------------------------------------
    // 右栏：选中项的详情
    // ------------------------------------------------------------------

    @Override
    public void renderDetails() {
        ImGuiShell.panelTitle("mcoo_build_with_ease.shell.details");

        var transformer = selectedTransformer();
        if (transformer == null) {
            ImGuiWidgets.diagnostic("(select a transformer on the left)");
            return;
        }

        ImGuiShell.fieldText("mcoo_build_with_ease.pattern.detail_type",
                transformer.getType().getDisplayName().getString());
        ImGuiShell.fieldText("mcoo_build_with_ease.pattern.detail_name", transformer.name().getString());
        ImGuiShell.fieldText("mcoo_build_with_ease.pattern.detail_valid",
                String.valueOf(transformer.isValid()));
        ImGuiShell.fieldText("mcoo_build_with_ease.pattern.detail_multiplier",
                String.format("%.2f×", transformer.volumeMultiplier()));

        ImGuiWidgets.separator();
        ImGuiWidgets.diagnostic(describe(transformer));
    }

    /**
     * 按类型给出一行参数摘要。
     * <p>
     * 用 instanceof 而不是给 {@link Transformer} 加方法：参数是每种类型自己的事，
     * 往公共接口加「取参数字符串」会把展示逻辑塞进业务模型里。
     */
    private static String describe(Transformer transformer) {
        if (transformer instanceof ArrayTransformer array) {
            var offset = array.offset();
            return "offset = " + offset.x() + ", " + offset.y() + ", " + offset.z()
                    + "   count = " + array.count();
        }
        if (transformer instanceof MirrorTransformer mirror) {
            var position = mirror.position();
            return "axis = " + mirror.axis()
                    + "   at = " + String.format("%.1f, %.1f, %.1f", position.x(), position.y(), position.z())
                    + "   size = " + mirror.size();
        }
        if (transformer instanceof RadialTransformer radial) {
            var position = radial.position();
            return "axis = " + radial.axis()
                    + "   at = " + String.format("%.1f, %.1f, %.1f", position.x(), position.y(), position.z())
                    + "   slices = " + radial.slices()
                    + "   radius = " + radial.radius()
                    + "   length = " + radial.length();
        }
        if (transformer instanceof ItemRandomizer randomizer) {
            return "order = " + randomizer.order()
                    + "   target = " + randomizer.target()
                    + "   source = " + randomizer.source()
                    + "   items = " + randomizer.chances().size();
        }
        return "";
    }

    // ------------------------------------------------------------------
    // 数据
    // ------------------------------------------------------------------

    private Pattern pattern() {
        var player = entrance.getClient().getPlayer();
        if (player == null) {
            return null;
        }
        return entrance.getStructureBuilder().getContext(player).pattern();
    }

    private void setPattern(Pattern pattern) {
        var player = entrance.getClient().getPlayer();
        if (player == null) {
            return;
        }
        entrance.getStructureBuilder().setPattern(player, pattern);
    }

    private Transformer selectedTransformer() {
        var pattern = pattern();
        if (pattern == null) {
            return null;
        }
        List<Transformer> transformers = pattern.transformers();
        if (transformers.isEmpty()) {
            return null;
        }
        if (selectedId == null) {
            selectedId = transformers.get(0).getId();
        }
        for (var transformer : transformers) {
            if (transformer.getId().equals(selectedId)) {
                return transformer;
            }
        }
        return transformers.get(0);
    }

}
