package com.mcoo.buildwithease.screen.imgui.panel;

import com.mcoo.buildwithease.McooBuildwithEaseClient;
import com.mcoo.buildwithease.building.config.PatternConfig;
import com.mcoo.buildwithease.building.pattern.Pattern;
import com.mcoo.buildwithease.building.pattern.Transformer;
import com.mcoo.buildwithease.building.pattern.Transformers;
import com.mcoo.buildwithease.building.pattern.array.ArrayTransformer;
import com.mcoo.buildwithease.building.pattern.mirror.MirrorTransformer;
import com.mcoo.buildwithease.building.pattern.raidal.RadialTransformer;
import com.mcoo.buildwithease.building.pattern.randomize.Chance;
import com.mcoo.buildwithease.building.pattern.randomize.ItemRandomizer;
import dev.huskuraft.universal.api.core.Item;
import dev.huskuraft.universal.vanilla.gui.McooImGuiItems;
import dev.huskuraft.universal.vanilla.gui.McooItemIconAtlas;
import dev.huskuraft.universal.vanilla.gui.McooImGuiTextures;
import imgui.type.ImString;
import com.mcoo.buildwithease.screen.imgui.widget.ImGuiShell;
import com.mcoo.buildwithease.screen.imgui.widget.ImGuiWidgets;

import java.util.List;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Collection;

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

    /** 调试用：置 true 后下一帧直接进物品选择页（免去脚本点按钮）。 */
    public static boolean DEBUG_START_PICKING;

    /** 是否正在给「物品随机器」挑物品（挑的时候中间栏换成物品网格）。 */
    private boolean pickingItems;
    /** 物品搜索框的缓冲（ImGui 要可变的字符串对象）。 */
    private final ImString searchText = new ImString("", 64);
    /** 搜索结果上限：再多也没人翻，还会拖慢每帧绘制。 */
    private static final int MAX_RESULTS = 240;
    /** 每行几个图标。 */
    private static final int ICON_COLUMNS = 14;
    private static final float ICON_SIZE = 26f;

    /** 当前选中的变换器 id（null = 默认选第一个）。 */
    private java.util.UUID selectedId;

    /** 正在从模板里挑变换器（替代旧的 TransformerPresetsSelectScreen）。 */
    private boolean pickingPreset;
    private Transformers presetType = Transformers.ARRAY;

    /** 调试用：置 true 后下一帧直接进模板选择页（免去脚本点按钮）。 */
    public static boolean DEBUG_START_PRESET;

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
        if (DEBUG_START_PICKING) {
            DEBUG_START_PICKING = false;
            pickingItems = true;
        }
        if (pickingItems) {
            renderItemPicker();
            return;
        }
        if (DEBUG_START_PRESET) {
            DEBUG_START_PRESET = false;
            pickingPreset = true;
        }
        if (pickingPreset) {
            renderPresetPicker();
            return;
        }
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
        if (ImGuiWidgets.button("mcoo_build_with_ease.pattern.add_transformer")) {
            pickingPreset = true;
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

    /**
     * 从内置模板 / 已保存预设里加一个变换器。
     * <p>
     * 旧流程是独立的模板选择界面；这里收进面板中间栏，按类型分组、点一下直接加进图案。
     */
    private void renderPresetPicker() {
        var pattern = pattern();
        if (pattern == null) {
            pickingPreset = false;
            return;
        }
        if (ImGuiWidgets.button("mcoo_build_with_ease.button.cancel")) {
            pickingPreset = false;
            return;
        }
        for (var type : Transformers.values()) {
            if (ImGuiShell.navItemRaw(type.getTitleText().getString(), type == presetType)) {
                presetType = type;
            }
        }
        ImGuiWidgets.separator();
        var selected = selectedTransformer();
        if (selected != null && ImGuiWidgets.button("mcoo_build_with_ease.pattern.save_preset")) {
            savePreset(selected);
        }
        ImGuiWidgets.separator();
        var builtIn = PatternConfig.getBuiltInPresets().getByType(presetType);
        for (var transformer : builtIn) {
            // 内置模板的 name() 是空的（名字是用户保存时才会填），所以标签用「类型 + 参数摘要」
            String label = "＋ " + transformLabel(transformer);
            if (ImGuiShell.navItemRaw(label, false)) {
                addTransformer(transformer);
            }
        }
        var saved = entrance.getConfigStorage().get().patternConfig().getByType(presetType);
        if (!saved.isEmpty()) {
            ImGuiWidgets.separator();
            ImGuiWidgets.text("mcoo_build_with_ease.pattern.saved_presets");
            for (var transformer : saved) {
                if (ImGuiShell.navItemRaw("＋ " + transformLabel(transformer), false)) {
                    addTransformer(transformer);
                }
            }
            ImGuiWidgets.separator();
            for (var transformer : saved) {
                if (imgui.ImGui.smallButton(tr("mcoo_build_with_ease.button.delete") + " " + transformLabel(transformer)
                        + "##preset_" + transformer.getId())) {
                    deletePreset(transformer);
                }
            }
        }
    }

    /** 模板/预设的可读标签：有名字用名字，没名字（内置项）退回「类型 + 参数摘要」。 */
    private static String transformLabel(Transformer transformer) {
        String name = transformer.name() == null ? "" : transformer.name().getString();
        if (!name.isEmpty()) {
            return name;
        }
        return transformer.getType().getDisplayName().getString() + "  " + describe(transformer);
    }

    /** 把当前选中的变换器存进 {@code PatternConfig} 的预设列表（旧的 PatternSettings 界面干的事）。 */
    private void savePreset(Transformer transformer) {
        var config = entrance.getConfigStorage().get();
        var presets = new ArrayList<Transformer>(config.patternConfig().transformerPreset());
        for (var existing : presets) {
            if (existing.getId().equals(transformer.getId())) {
                return;
            }
        }
        presets.add(transformer);
        entrance.getConfigStorage().update(c -> c.withPatternConfig(new PatternConfig(presets)));
    }

    private void deletePreset(Transformer transformer) {
        var config = entrance.getConfigStorage().get();
        var presets = new ArrayList<Transformer>(config.patternConfig().transformerPreset());
        presets.removeIf(existing -> existing.getId().equals(transformer.getId()));
        entrance.getConfigStorage().update(c -> c.withPatternConfig(new PatternConfig(presets)));
    }

    private static String tr(String key) {
        return dev.huskuraft.universal.api.text.Text.translate(key).getString();
    }

    private void addTransformer(Transformer transformer) {        var pattern = pattern();
        if (pattern == null || transformer == null) {
            return;
        }
        var updated = new ArrayList<Transformer>(pattern.transformers());
        // 同一个模板连点两次会撞 id（Transformer 按 id 做替换语义），直接忽略重复
        for (var existing : updated) {
            if (existing.getId().equals(transformer.getId())) {
                pickingPreset = false;
                return;
            }
        }
        updated.add(transformer);
        setPattern(pattern.withTransformers(updated));
        selectedId = transformer.getId();
        pickingPreset = false;
    }

    private void removeSelectedTransformer() {
        var pattern = pattern();
        if (pattern == null || selectedId == null) {
            return;
        }
        var updated = new ArrayList<Transformer>(pattern.transformers());
        updated.removeIf(transformer -> transformer.getId().equals(selectedId));
        setPattern(pattern.withTransformers(updated));
        selectedId = updated.isEmpty() ? null : updated.get(0).getId();
    }

    /** 上移/下移（delta = -1 / +1）。顺序就是建造时的套用顺序，所以放这里而不是列表拖拽（ImGui 拖拽要额外依赖）。 */
    private void moveSelected(int delta) {
        var pattern = pattern();
        if (pattern == null || selectedId == null) {
            return;
        }
        var updated = new ArrayList<Transformer>(pattern.transformers());
        int index = -1;
        for (int i = 0; i < updated.size(); i++) {
            if (updated.get(i).getId().equals(selectedId)) {
                index = i;
                break;
            }
        }
        int target = index + delta;
        if (index < 0 || target < 0 || target >= updated.size()) {
            return;
        }
        var tmp = updated.get(index);
        updated.set(index, updated.get(target));
        updated.set(target, tmp);
        setPattern(pattern.withTransformers(updated));
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

        if (ImGuiWidgets.button("mcoo_build_with_ease.pattern.delete_transformer")) {
            removeSelectedTransformer();
            return;
        }
        if (imgui.ImGui.smallButton(tr("mcoo_build_with_ease.button.up") + "##move_up")) {
            moveSelected(-1);
        }
        imgui.ImGui.sameLine();
        if (imgui.ImGui.smallButton(tr("mcoo_build_with_ease.button.down") + "##move_down")) {
            moveSelected(1);
        }
        imgui.ImGui.sameLine();
        if (imgui.ImGui.smallButton(tr("mcoo_build_with_ease.button.clear") + "##clear_all")) {
            var pattern = pattern();
            if (pattern != null) {
                setPattern(pattern.withTransformers(List.of()));
                selectedId = null;
            }
            return;
        }
        ImGuiWidgets.separator();

        ImGuiShell.fieldText("mcoo_build_with_ease.pattern.detail_type",
                transformer.getType().getDisplayName().getString());
        ImGuiShell.fieldText("mcoo_build_with_ease.pattern.detail_name", transformer.name().getString());
        ImGuiShell.fieldText("mcoo_build_with_ease.pattern.detail_valid",
                String.valueOf(transformer.isValid()));
        ImGuiShell.fieldText("mcoo_build_with_ease.pattern.detail_multiplier",
                String.format("%.2f×", transformer.volumeMultiplier()));

        ImGuiWidgets.separator();
        renderEditor(transformer);
    }

    /**
     * 按类型画参数表单。
     * <p>
     * 每种变换器都是不可变 record，参数改了要<b>换一个实例</b>再放回图案里 ——
     * 所以统一走 {@link #replace(Transformer, Transformer)}：先用 withXxx 造新值，再整体写回。
     */
    private void renderEditor(Transformer transformer) {
        if (transformer instanceof ArrayTransformer array) {
            int x = ImGuiShell.fieldSliderInt("mcoo_build_with_ease.pattern.offset_x",
                    array.offset().x(), -64, 64);
            int y = ImGuiShell.fieldSliderInt("mcoo_build_with_ease.pattern.offset_y",
                    array.offset().y(), -64, 64);
            int z = ImGuiShell.fieldSliderInt("mcoo_build_with_ease.pattern.offset_z",
                    array.offset().z(), -64, 64);
            int count = ImGuiShell.fieldSliderInt("mcoo_build_with_ease.pattern.count",
                    array.count(), 1, 64);
            if (x != array.offset().x() || y != array.offset().y() || z != array.offset().z()) {
                replace(transformer, array.withOffset(new dev.huskuraft.universal.api.math.Vector3i(x, y, z)));
            } else if (count != array.count()) {
                replace(transformer, array.withCount(count));
            }
            return;
        }
        if (transformer instanceof MirrorTransformer mirror) {
            var axis = ImGuiShell.fieldCombo("mcoo_build_with_ease.pattern.axis",
                    AXIS_NAMES, mirror.axis().ordinal());
            int size = ImGuiShell.fieldSliderInt("mcoo_build_with_ease.pattern.size", mirror.size(), 1, 64);
            if (axis != mirror.axis().ordinal()) {
                replace(transformer, mirror.withAxis(AXIS_VALUES[axis]));
            } else if (size != mirror.size()) {
                replace(transformer, mirror.withSize(size));
            }
            return;
        }
        if (transformer instanceof RadialTransformer radial) {
            var axis = ImGuiShell.fieldCombo("mcoo_build_with_ease.pattern.axis",
                    AXIS_NAMES, radial.axis().ordinal());
            int slices = ImGuiShell.fieldSliderInt("mcoo_build_with_ease.pattern.slices", radial.slices(), 2, 64);
            int radius = ImGuiShell.fieldSliderInt("mcoo_build_with_ease.pattern.radius", radial.radius(), 1, 256);
            int length = ImGuiShell.fieldSliderInt("mcoo_build_with_ease.pattern.length", radial.length(), 1, 256);
            if (axis != radial.axis().ordinal()) {
                replace(transformer, radial.withAxis(AXIS_VALUES[axis]));
            } else if (slices != radial.slices()) {
                replace(transformer, radial.withSlice(slices));
            } else if (radius != radial.radius()) {
                replace(transformer, radial.withRadius(radius));
            } else if (length != radial.length()) {
                replace(transformer, radial.withLength(length));
            }
            return;
        }
        if (transformer instanceof ItemRandomizer randomizer) {
            // 物品随机器真正的编辑是「物品 + 概率」表，那是物品选择页的活；
            // 这里只调三个枚举维度（顺序 / 目标 / 来源），配合下面的物品数只读展示。
            var order = ImGuiShell.fieldCombo("mcoo_build_with_ease.pattern.randomizer_order",
                    enumNames(ItemRandomizer.Order.values()), randomizer.order().ordinal());
            var target = ImGuiShell.fieldCombo("mcoo_build_with_ease.pattern.randomizer_target",
                    enumNames(ItemRandomizer.Target.values()), randomizer.target().ordinal());
            var source = ImGuiShell.fieldCombo("mcoo_build_with_ease.pattern.randomizer_source",
                    enumNames(ItemRandomizer.Source.values()), randomizer.source().ordinal());
            if (order != randomizer.order().ordinal()) {
                replace(transformer, randomizer.withOrder(ItemRandomizer.Order.values()[order]));
            } else if (target != randomizer.target().ordinal()) {
                replace(transformer, randomizer.withTarget(ItemRandomizer.Target.values()[target]));
            } else if (source != randomizer.source().ordinal()) {
                replace(transformer, randomizer.withSource(ItemRandomizer.Source.values()[source]));
            }
            ImGuiWidgets.separator();
            renderChances(randomizer);
            return;
        }
        ImGuiWidgets.diagnostic(describe(transformer));
    }

    // ------------------------------------------------------------------
    // 物品随机器：概率表 + 物品选择
    // ------------------------------------------------------------------

    /** 右栏：概率表（图标 + 名字 + 概率滑条 + 删除）。 */
    private void renderChances(ItemRandomizer randomizer) {
        var chances = new ArrayList<Chance<Item>>(randomizer.chances());
        if (chances.isEmpty()) {
            ImGuiWidgets.diagnostic("(还没有物品)");
        }

        long atlasId = McooImGuiTextures.blockAtlasId();
        Chance<Item> toRemove = null;
        Chance<Item> toUpdate = null;
        int newChance = 0;

        for (int i = 0; i < chances.size(); i++) {
            var chance = chances.get(i);
            var itemId = chance.content().getId().getString();
            var chanceUv = McooImGuiItems.iconUv(itemId);
            if (chanceUv == null) {
                imgui.ImGui.dummy(16f, 16f);
            } else {
                ImGuiWidgets.image(McooImGuiItems.iconTextureId(itemId), 16f, 16f, chanceUv);
            }
            imgui.ImGui.sameLine(0f, 6f);
            imgui.ImGui.alignTextToFramePadding();
            imgui.ImGui.text(McooImGuiItems.displayName(itemId));
            imgui.ImGui.sameLine(0f, 6f);
            imgui.ImGui.setNextItemWidth(70f);
            int[] holder = {chance.chance()};
            imgui.ImGui.sliderInt("##chance_" + i, holder, Chance.MIN_ITEM_COUNT, Chance.MAX_ITEM_COUNT);
            if (holder[0] != chance.chance()) {
                toUpdate = chance;
                newChance = holder[0];
            }
            imgui.ImGui.sameLine(0f, 4f);
            if (imgui.ImGui.smallButton("x##del_" + i)) {
                toRemove = chance;
            }
        }

        // 改动在这里统一提交：循环里改集合会 ConcurrentModificationException
        if (toRemove != null) {
            var updated = new ArrayList<>(chances);
            updated.remove(toRemove);
            replace(randomizer, randomizer.withChances(updated));
            return;
        }
        if (toUpdate != null) {
            var updated = new ArrayList<Chance<Item>>();
            for (var chance : chances) {
                updated.add(chance == toUpdate ? Chance.item(toUpdate.content(), newChance) : chance);
            }
            replace(randomizer, randomizer.withChances(updated));
            return;
        }

        if (ImGuiWidgets.button("mcoo_build_with_ease.pattern.add_item")) {
            searchText.set("");
            pickingItems = true;
        }
    }

    /**
     * 中间栏：物品选择。
     * <p>
     * 复用 {@code ClientContentFactory} 的物品搜索树（和自研的物品选择界面同一套数据源），
     * 结果画成图标网格 —— 点一下就加进当前随机器的概率表。
     */
    private void renderItemPicker() {
        var randomizer = selectedRandomizer();
        if (randomizer == null) {
            pickingItems = false;
            return;
        }

        ImGuiWidgets.text("mcoo_build_with_ease.pattern.pick_item");
        imgui.ImGui.sameLine();
        if (ImGuiWidgets.button("mcoo_build_with_ease.pattern.pick_done")) {
            pickingItems = false;
            return;
        }

        imgui.ImGui.setNextItemWidth(-1f);
        imgui.ImGui.inputTextWithHint("##item_search", "搜索物品（支持名字）", searchText);

        var results = searchItems(searchText.get());
        // 诊断：注册表里有多少物品、图集 id —— 空网格时先看这两个数
        ImGuiWidgets.diagnostic("items = " + McooImGuiItems.allItemIds().size()
                + "  shown = " + results.size()
                + "  atlas = " + McooImGuiTextures.blockAtlasId()
                + "  icon = " + (McooItemIconAtlas.get() == null ? "未建" : "ok")
                + "  baked = " + McooItemIconAtlas.bakedCount()
                + (results.isEmpty() ? ""
                    : "  id = " + McooImGuiItems.iconTextureId(results.get(0))
                      + "  uv = " + java.util.Arrays.toString(McooImGuiItems.iconUv(results.get(0))))
                + (McooItemIconAtlas.lastError() == null ? "" : "  err = " + McooItemIconAtlas.lastError()));
        String picked = null;
        int nullUv = 0;
        int zeroTex = 0;
        // 纹理 id 每帧取一次就够：宿主按 view 身份映射，但每格都调一次没有意义，
        // 也排除「每调一次就换一个 id」这种宿主的边界行为。
        long iconTexture = McooImGuiItems.iconTextureId("minecraft:stone");
        for (int i = 0; i < results.size(); i++) {
            if (i % ICON_COLUMNS != 0) {
                imgui.ImGui.sameLine(0f, 3f);
            }
            var itemId = results.get(i);
            // 拿不到图标就画个空占位：直接传 null 会让 ImGuiWidgets 把**整张图集**缩进 26px
            // （看起来像一片噪点/棋盘，很难看出是"没图标"）—— 踩过。
            var iconUv = McooImGuiItems.iconUv(itemId);
            if (iconUv == null) {
                nullUv++;
                imgui.ImGui.dummy(ICON_SIZE, ICON_SIZE);
            } else if (iconTexture == 0L) {
                zeroTex++;
                imgui.ImGui.dummy(ICON_SIZE, ICON_SIZE);
            } else {
                ImGuiWidgets.image(iconTexture, ICON_SIZE, ICON_SIZE, iconUv);
            }
            if (i == 0) {
                ImGuiWidgets.diagnostic("  #0 item=" + itemId + " tex=" + iconTexture + " uv="
                        + (iconUv == null ? "null" : iconUv[0] + "," + iconUv[1]));
                // 决定性实验：同一个 UV 放大到 100px，排除「小尺寸画不出来」的可能
                if (iconUv != null && iconTexture != 0L) {
                    ImGuiWidgets.image(iconTexture, 100f, 100f, iconUv);
                }
            }
            // 悬停提示：图标网格只靠图标猜不出是哪个物品
            if (imgui.ImGui.isItemHovered()) {
                imgui.ImGui.setTooltip(McooImGuiItems.displayName(itemId));
            }
            if (imgui.ImGui.isItemClicked()) {
                picked = itemId;
            }
        }

        ImGuiWidgets.diagnostic("  nullUv=" + nullUv + " zeroTex=" + zeroTex);

        if (picked != null) {
            // 字符串 id → api 的 Item：业务层不认识 MC 注册表，靠 ContentFactory 换
            var location = dev.huskuraft.universal.api.core.ResourceLocation.decompose(picked);
            var item = dev.huskuraft.universal.api.platform.ContentFactory.getInstance()
                    .newOptionalItem(location).orElse(null);
            if (item != null) {
                var updated = new ArrayList<>(randomizer.chances());
                updated.add(Chance.item(item, 1));
                replace(randomizer, randomizer.withChances(updated));
                pickingItems = false;
            }
        }
    }

    /**
     * 按名字过滤物品（空查询 = 全部）。
     * <p>
     * 数据来自**物品注册表**（{@code McooImGuiItems}），不是创造模式搜索树 ——
     * 后者空查询返回空、还依赖玩家连接（详见 McooImGuiItems 的注释）。
     */
    private static List<String> searchItems(String query) {
        var normalized = query == null ? "" : query.toLowerCase(Locale.ROOT).trim();
        var all = McooImGuiItems.allItemIds();
        var result = new ArrayList<String>();
        for (var id : all) {
            if (normalized.isEmpty() || id.contains(normalized)) {
                result.add(id);
                if (result.size() >= MAX_RESULTS) {
                    break;
                }
            }
        }
        return result;
    }

    /** 当前选中的变换器如果是物品随机器，就返回它（挑物品时要用）。 */
    private ItemRandomizer selectedRandomizer() {
        var transformer = selectedTransformer();
        return transformer instanceof ItemRandomizer randomizer ? randomizer : null;
    }

    private static final dev.huskuraft.universal.api.core.Axis[] AXIS_VALUES =
            dev.huskuraft.universal.api.core.Axis.values();
    private static final String[] AXIS_NAMES = enumNames(AXIS_VALUES);

    private static String[] enumNames(Enum<?>[] values) {
        String[] names = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            names[i] = values[i].name();
        }
        return names;
    }

    /** 把一个变换器换成改好的新实例（record 不可变，只能整体替换）。 */
    private void replace(Transformer oldTransformer, Transformer newTransformer) {
        var pattern = pattern();
        if (pattern == null) {
            return;
        }
        var replacements = new java.util.ArrayList<Transformer>(pattern.transformers());
        for (int i = 0; i < replacements.size(); i++) {
            if (replacements.get(i).getId().equals(oldTransformer.getId())) {
                replacements.set(i, newTransformer);
                break;
            }
        }
        setPattern(pattern.withTransformers(replacements));
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
