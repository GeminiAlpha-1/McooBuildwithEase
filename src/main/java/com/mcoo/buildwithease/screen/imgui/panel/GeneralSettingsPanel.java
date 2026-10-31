package com.mcoo.buildwithease.screen.imgui.panel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import com.mcoo.buildwithease.McooBuildwithEaseClient;
import com.mcoo.buildwithease.networking.packets.player.PlayerPermissionCheckPacket;
import com.mcoo.buildwithease.screen.imgui.widget.ImGuiShell;
import com.mcoo.buildwithease.screen.imgui.widget.ImGuiWidgets;
import com.mcoo.buildwithease.session.config.ConstraintConfig;

import dev.huskuraft.universal.api.core.PlayerInfo;
import dev.huskuraft.universal.api.core.ResourceLocation;
import dev.huskuraft.universal.api.text.Text;
import dev.huskuraft.universal.vanilla.gui.McooImGuiItems;
import imgui.type.ImString;

/**
 * 通用设置面板：<b>全局约束</b> + <b>每玩家覆盖</b>，替代旧的四张自研界面
 * （设置总页 / 通用设置 / 全局约束 / 玩家列表）。
 *
 * <h3>写路径为什么要过权限</h3>
 * 服务端限制是「全局一份 + 每玩家可选覆盖」。改全局要过 {@link PlayerPermissionCheckPacket}
 * （异步往返），没权限时旧界面是弹「不是操作员」。这里用同一条链路：点「编辑全局」时发校验，
 * 只有 granted 才进入编辑态。
 *
 * <h3>草稿 + 保存</h3>
 * 编辑时改的是本地 {@link Edit} 草稿，只有点「保存」才 {@code updateGlobalConfig} / {@code updatePlayerConfig}。
 * 这样一次拖滑条不会打一串包，也允许「取消」。
 *
 * <h3>每玩家覆盖的三态</h3>
 * {@code ConstraintConfig} 的字段是装箱类型：{@code null} = 沿用全局，非空 = 覆盖。
 * 所以玩家编辑里布尔用「继承 / 开 / 关」三选一，整数用「继承」勾选 + 滑条。
 */
public final class GeneralSettingsPanel implements ImGuiPanel {

    private static final String ID = "settings";

    private static final String KEY_ALLOW_USE_MOD = "mcoo_build_with_ease.general_settings.allow_use_mod";
    private static final String KEY_ALLOW_BREAK = "mcoo_build_with_ease.general_settings.allow_break_blocks";
    private static final String KEY_ALLOW_PLACE = "mcoo_build_with_ease.general_settings.allow_place_blocks";
    private static final String KEY_ALLOW_INTERACT = "mcoo_build_with_ease.general_settings.allow_interact_blocks";
    private static final String KEY_ALLOW_COPY_PASTE = "mcoo_build_with_ease.general_settings.allow_copy_paste_structures";
    private static final String KEY_PROPER_TOOLS = "mcoo_build_with_ease.general_settings.use_proper_tools";
    private static final String KEY_MAX_REACH = "mcoo_build_with_ease.general_settings.max_reach_distance";
    private static final String KEY_MAX_BREAK = "mcoo_build_with_ease.general_settings.max_block_break_volume";
    private static final String KEY_MAX_PLACE = "mcoo_build_with_ease.general_settings.max_block_place_volume";
    private static final String KEY_MAX_INTERACT = "mcoo_build_with_ease.general_settings.max_block_interact_volume";
    private static final String KEY_MAX_COPY = "mcoo_build_with_ease.general_settings.max_structure_copy_paste_volume";
    private static final String KEY_WHITELIST = "mcoo_build_with_ease.general_settings.whitelisted_items";
    private static final String KEY_BLACKLIST = "mcoo_build_with_ease.general_settings.blacklisted_items";

    private final McooBuildwithEaseClient entrance;

    /** 全局草稿（null = 只读浏览态）。 */
    private Edit globalEdit;
    /** 正在编辑的玩家（null = 没在编辑玩家覆盖）。 */
    private UUID playerEditId;
    private Edit playerEdit;
    /** 本次会话是否已通过权限校验（只用于显示，真正的把关在服务端）。 */
    private boolean granted;
    private String status;
    /** 0 = 没在选物品；1 = 白名单；2 = 黑名单。 */
    private int pickingList;
    private final ImString search = new ImString("", 64);

    public GeneralSettingsPanel(McooBuildwithEaseClient entrance) {
        this.entrance = entrance;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String titleKey() {
        return "mcoo_build_with_ease.general_settings.title";
    }

    // ------------------------------------------------------------------
    // 中间栏
    // ------------------------------------------------------------------

    @Override
    public void renderContent() {
        var session = entrance.getSessionManager();
        if (!session.isSessionValid()) {
            ImGuiWidgets.text("mcoo_build_with_ease.player_list.no_session");
            return;
        }
        var config = session.getServerSessionConfigOrEmpty();

        // 正在选物品时，中间栏整块让给物品网格（和图案面板同一个交互）
        if (pickingList != 0) {
            renderItemPicker();
            return;
        }

        ImGuiShell.section("mcoo_build_with_ease.general_settings.global");
        if (globalEdit == null) {
            renderSummary(config.getGlobalConfig());
            if (ImGuiWidgets.button("mcoo_build_with_ease.general_settings.edit_global")) {
                requestPermission(() -> globalEdit = new Edit(config.getGlobalConfig()));
            }
        } else {
            renderGlobalEditor(globalEdit);
            ImGuiWidgets.spacing();
            if (ImGuiWidgets.button("mcoo_build_with_ease.button.save")) {
                boolean ok = session.updateGlobalConfig(globalEdit.toConfig());
                status = ok ? tr("mcoo_build_with_ease.general_settings.saved") : tr("mcoo_build_with_ease.general_settings.save_failed");
                if (ok) {
                    globalEdit = null;
                }
            }
            imgui.ImGui.sameLine();
            if (ImGuiWidgets.button("mcoo_build_with_ease.button.reset")) {
                globalEdit = new Edit(ConstraintConfig.DEFAULT);
            }
            imgui.ImGui.sameLine();
            if (ImGuiWidgets.button("mcoo_build_with_ease.button.cancel")) {
                globalEdit = null;
            }
        }

        ImGuiWidgets.separator();
        ImGuiShell.section("mcoo_build_with_ease.general_settings.per_player");
        renderPlayerOverrides(config.playerConfigs());

        if (status != null) {
            ImGuiWidgets.spacing();
            ImGuiWidgets.diagnostic(status);
        }
    }

    @Override
    public void renderDetails() {
        ImGuiShell.panelTitle("mcoo_build_with_ease.shell.details");
        ImGuiWidgets.textWrapped("mcoo_build_with_ease.general_settings.help");
        ImGuiWidgets.separator();
        ImGuiWidgets.labelText("mcoo_build_with_ease.general_settings.permission",
                granted ? tr("mcoo_build_with_ease.general_settings.permission_granted")
                        : tr("mcoo_build_with_ease.general_settings.permission_missing"));
        if (status != null) {
            ImGuiWidgets.separator();
            ImGuiWidgets.diagnostic(status);
        }
    }

    // ------------------------------------------------------------------
    // 全局编辑
    // ------------------------------------------------------------------

    private void renderGlobalEditor(Edit edit) {
        edit.allowUseMod = ImGuiShell.fieldCheckbox(KEY_ALLOW_USE_MOD, bool(edit.allowUseMod));
        edit.allowBreakBlocks = ImGuiShell.fieldCheckbox(KEY_ALLOW_BREAK, bool(edit.allowBreakBlocks));
        edit.allowPlaceBlocks = ImGuiShell.fieldCheckbox(KEY_ALLOW_PLACE, bool(edit.allowPlaceBlocks));
        edit.allowInteractBlocks = ImGuiShell.fieldCheckbox(KEY_ALLOW_INTERACT, bool(edit.allowInteractBlocks));
        edit.allowCopyPasteStructures = ImGuiShell.fieldCheckbox(KEY_ALLOW_COPY_PASTE, bool(edit.allowCopyPasteStructures));
        edit.useProperToolsOnly = ImGuiShell.fieldCheckbox(KEY_PROPER_TOOLS, bool(edit.useProperToolsOnly));
        edit.maxReachDistance = ImGuiShell.fieldSliderInt(KEY_MAX_REACH, integer(edit.maxReachDistance),
                ConstraintConfig.MAX_REACH_DISTANCE_RANGE_START, ConstraintConfig.MAX_REACH_DISTANCE_RANGE_END);
        edit.maxBlockBreakVolume = ImGuiShell.fieldSliderInt(KEY_MAX_BREAK, integer(edit.maxBlockBreakVolume),
                ConstraintConfig.MAX_BLOCK_BREAK_VOLUME_RANGE_START, ConstraintConfig.MAX_BLOCK_BREAK_VOLUME_RANGE_END);
        edit.maxBlockPlaceVolume = ImGuiShell.fieldSliderInt(KEY_MAX_PLACE, integer(edit.maxBlockPlaceVolume),
                ConstraintConfig.MAX_BLOCK_PLACE_VOLUME_RANGE_START, ConstraintConfig.MAX_BLOCK_PLACE_VOLUME_RANGE_END);
        edit.maxBlockInteractVolume = ImGuiShell.fieldSliderInt(KEY_MAX_INTERACT, integer(edit.maxBlockInteractVolume),
                ConstraintConfig.MAX_BLOCK_INTERACT_VOLUME_RANGE_START, ConstraintConfig.MAX_BLOCK_INTERACT_VOLUME_RANGE_END);
        edit.maxStructureCopyPasteVolume = ImGuiShell.fieldSliderInt(KEY_MAX_COPY, integer(edit.maxStructureCopyPasteVolume),
                ConstraintConfig.MAX_STRUCTURE_COPY_PASTE_VOLUME_RANGE_START, ConstraintConfig.MAX_STRUCTURE_COPY_PASTE_VOLUME_RANGE_END);

        renderListRow(KEY_WHITELIST, edit.whitelistedItems, () -> {
            pickingList = 1;
            edit.targetListIsWhitelist = true;
        });
        renderListRow(KEY_BLACKLIST, edit.blacklistedItems, () -> {
            pickingList = 2;
            edit.targetListIsWhitelist = false;
        });
    }

    private void renderSummary(ConstraintConfig config) {
        ImGuiWidgets.diagnostic(tr(KEY_ALLOW_USE_MOD) + " = " + value(config.allowUseMod()));
        ImGuiWidgets.diagnostic(tr(KEY_ALLOW_BREAK) + " = " + value(config.allowBreakBlocks()));
        ImGuiWidgets.diagnostic(tr(KEY_ALLOW_PLACE) + " = " + value(config.allowPlaceBlocks()));
        ImGuiWidgets.diagnostic(tr(KEY_MAX_REACH) + " = " + num(config.maxReachDistance()));
        ImGuiWidgets.diagnostic(tr(KEY_MAX_COPY) + " = " + num(config.maxStructureCopyPasteVolume()));
        if (config.whitelistedItems() != null && !config.whitelistedItems().isEmpty()) {
            ImGuiWidgets.diagnostic(tr(KEY_WHITELIST) + " = " + config.whitelistedItems().size());
        }
        if (config.blacklistedItems() != null && !config.blacklistedItems().isEmpty()) {
            ImGuiWidgets.diagnostic(tr(KEY_BLACKLIST) + " = " + config.blacklistedItems().size());
        }
    }

    // ------------------------------------------------------------------
    // 每玩家覆盖
    // ------------------------------------------------------------------

    private void renderPlayerOverrides(Map<UUID, ConstraintConfig> configs) {
        var players = players();
        // 在线玩家 + 已有覆盖的离线玩家都要能看到（覆盖可能是在玩家离线时设定的）
        var merged = new LinkedHashMap<UUID, String>();
        for (var player : players) {
            var profile = player.getProfile();
            if (profile != null && profile.getId() != null) {
                merged.put(profile.getId(), profile.getName() == null ? "?" : profile.getName());
            }
        }
        for (var id : configs.keySet()) {
            merged.putIfAbsent(id, id.toString().substring(0, 8));
        }
        if (merged.isEmpty()) {
            ImGuiWidgets.diagnostic(tr("mcoo_build_with_ease.general_settings.no_players"));
            return;
        }

        if (playerEditId != null && playerEdit != null) {
            ImGuiWidgets.diagnostic(tr("mcoo_build_with_ease.general_settings.editing_player",
                    merged.getOrDefault(playerEditId, playerEditId.toString().substring(0, 8))));
            renderPlayerEditor(playerEdit);
            ImGuiWidgets.spacing();
            if (ImGuiWidgets.button("mcoo_build_with_ease.button.save")) {
                boolean ok = entrance.getSessionManager().updatePlayerConfig(playerEditId, playerEdit.toConfig());
                status = ok ? tr("mcoo_build_with_ease.general_settings.saved") : tr("mcoo_build_with_ease.general_settings.save_failed");
                if (ok) {
                    playerEditId = null;
                    playerEdit = null;
                }
            }
            imgui.ImGui.sameLine();
            if (ImGuiWidgets.button("mcoo_build_with_ease.button.reset")) {
                playerEdit = new Edit(ConstraintConfig.NULL);
            }
            imgui.ImGui.sameLine();
            if (ImGuiWidgets.button("mcoo_build_with_ease.button.cancel")) {
                playerEditId = null;
                playerEdit = null;
            }
            return;
        }

        for (var entry : merged.entrySet()) {
            var override = configs.get(entry.getKey());
            String label = (override == null ? "○ " : "● ") + entry.getValue();
            if (ImGuiShell.navItemRaw(label, false)) {
                requestPermission(() -> {
                    playerEditId = entry.getKey();
                    playerEdit = new Edit(override == null ? ConstraintConfig.NULL : override);
                });
            }
        }
    }

    private void renderPlayerEditor(Edit edit) {
        edit.allowUseMod = triState(KEY_ALLOW_USE_MOD, edit.allowUseMod);
        edit.allowBreakBlocks = triState(KEY_ALLOW_BREAK, edit.allowBreakBlocks);
        edit.allowPlaceBlocks = triState(KEY_ALLOW_PLACE, edit.allowPlaceBlocks);
        edit.allowInteractBlocks = triState(KEY_ALLOW_INTERACT, edit.allowInteractBlocks);
        edit.allowCopyPasteStructures = triState(KEY_ALLOW_COPY_PASTE, edit.allowCopyPasteStructures);
        edit.useProperToolsOnly = triState(KEY_PROPER_TOOLS, edit.useProperToolsOnly);

        edit.maxReachDistance = triInt(KEY_MAX_REACH, edit.maxReachDistance,
                ConstraintConfig.MAX_REACH_DISTANCE_RANGE_START, ConstraintConfig.MAX_REACH_DISTANCE_RANGE_END);
        edit.maxBlockBreakVolume = triInt(KEY_MAX_BREAK, edit.maxBlockBreakVolume,
                ConstraintConfig.MAX_BLOCK_BREAK_VOLUME_RANGE_START, ConstraintConfig.MAX_BLOCK_BREAK_VOLUME_RANGE_END);
        edit.maxBlockPlaceVolume = triInt(KEY_MAX_PLACE, edit.maxBlockPlaceVolume,
                ConstraintConfig.MAX_BLOCK_PLACE_VOLUME_RANGE_START, ConstraintConfig.MAX_BLOCK_PLACE_VOLUME_RANGE_END);
        edit.maxBlockInteractVolume = triInt(KEY_MAX_INTERACT, edit.maxBlockInteractVolume,
                ConstraintConfig.MAX_BLOCK_INTERACT_VOLUME_RANGE_START, ConstraintConfig.MAX_BLOCK_INTERACT_VOLUME_RANGE_END);
        edit.maxStructureCopyPasteVolume = triInt(KEY_MAX_COPY, edit.maxStructureCopyPasteVolume,
                ConstraintConfig.MAX_STRUCTURE_COPY_PASTE_VOLUME_RANGE_START, ConstraintConfig.MAX_STRUCTURE_COPY_PASTE_VOLUME_RANGE_END);

        renderListRow(KEY_WHITELIST, edit.whitelistedItems, () -> {
            pickingList = 1;
            edit.targetListIsWhitelist = true;
        });
        renderListRow(KEY_BLACKLIST, edit.blacklistedItems, () -> {
            pickingList = 2;
            edit.targetListIsWhitelist = false;
        });
        ImGuiWidgets.diagnostic(tr("mcoo_build_with_ease.general_settings.override_hint"));
    }

    private static Boolean triState(String labelKey, Boolean value) {
        String[] items = {
                tr("mcoo_build_with_ease.general_settings.inherit"),
                tr("mcoo_build_with_ease.general_settings.on"),
                tr("mcoo_build_with_ease.general_settings.off")
        };
        int current = value == null ? 0 : (value ? 1 : 2);
        int selected = ImGuiShell.fieldCombo(labelKey, items, current);
        return switch (selected) {
            case 1 -> Boolean.TRUE;
            case 2 -> Boolean.FALSE;
            default -> null;
        };
    }

    private static Integer triInt(String labelKey, Integer value, int min, int max) {
        // 用原生 checkbox + ##id：fieldCheckbox 只收 i18n key，这里需要「同一标签、不同 ID」
        boolean inherit = imgui.ImGui.checkbox(tr("mcoo_build_with_ease.general_settings.inherit") + "##" + labelKey,
                value == null);
        if (inherit) {
            return null;
        }
        return ImGuiShell.fieldSliderInt(labelKey, value == null ? min : value, min, max);
    }

    private void renderListRow(String labelKey, List<ResourceLocation> list, Runnable onEdit) {
        int count = list == null ? 0 : list.size();
        ImGuiWidgets.labelText(labelKey, Text.translate("mcoo_build_with_ease.general_settings.items", String.valueOf(count)).getString());
        imgui.ImGui.sameLine();
        if (ImGuiWidgets.button("mcoo_build_with_ease.button.edit")) {
            onEdit.run();
        }
    }

    // ------------------------------------------------------------------
    // 物品选择（网格复刻图案面板的那套）
    // ------------------------------------------------------------------

    private void renderItemPicker() {
        Edit target = playerEdit != null ? playerEdit : globalEdit;
        if (target == null) {
            pickingList = 0;
            return;
        }
        List<ResourceLocation> list;
        if (pickingList == 1) {
            list = target.whitelistedItems == null ? new ArrayList<>() : new ArrayList<>(target.whitelistedItems);
        } else {
            list = target.blacklistedItems == null ? new ArrayList<>() : new ArrayList<>(target.blacklistedItems);
        }

        if (ImGuiWidgets.button("mcoo_build_with_ease.button.done")) {
            if (pickingList == 1) {
                target.whitelistedItems = list;
            } else {
                target.blacklistedItems = list;
            }
            pickingList = 0;
            return;
        }
        imgui.ImGui.sameLine();
        ImGuiWidgets.diagnostic(tr("mcoo_build_with_ease.general_settings.items", String.valueOf(list.size())));

        imgui.ImGui.setNextItemWidth(-1f);
        imgui.ImGui.inputTextWithHint("##general_item_search", tr("mcoo_build_with_ease.general_settings.search_hint"), search);

        var results = searchItems(search.get());
        long iconTexture = McooImGuiItems.iconTextureId("minecraft:stone");
        for (int i = 0; i < results.size(); i++) {
            if (i % 14 != 0) {
                imgui.ImGui.sameLine(0f, 3f);
            }
            String itemId = results.get(i);
            float[] uv = McooImGuiItems.iconUv(itemId);
            boolean selected = containsId(list, itemId);
            if (uv == null || iconTexture == 0L) {
                // 图集还没烘出来：占位方块仍然可点（按 id 记进名单）
                if (imgui.ImGui.button("##general_item_" + i, 24f, 24f)) {
                    toggle(list, itemId);
                }
            } else {
                ImGuiWidgets.image(McooImGuiItems.iconTextureId(itemId), 24f, 24f, uv);
                if (imgui.ImGui.isItemClicked()) {
                    toggle(list, itemId);
                }
            }
            if (imgui.ImGui.isItemHovered()) {
                imgui.ImGui.setTooltip(McooImGuiItems.displayName(itemId) + (selected ? "  ●" : ""));
            }
        }
    }

    private static List<String> searchItems(String query) {
        String normalized = query == null ? "" : query.toLowerCase(Locale.ROOT).trim();
        var all = McooImGuiItems.allItemIds();
        var result = new ArrayList<String>();
        for (var id : all) {
            if (normalized.isEmpty() || id.contains(normalized)) {
                result.add(id);
                if (result.size() >= 240) {
                    break;
                }
            }
        }
        return result;
    }

    private static boolean containsId(List<ResourceLocation> list, String itemId) {
        for (var location : list) {
            if (location.getString().equals(itemId)) {
                return true;
            }
        }
        return false;
    }

    private static void toggle(List<ResourceLocation> list, String itemId) {
        for (var iterator = list.iterator(); iterator.hasNext(); ) {
            if (iterator.next().getString().equals(itemId)) {
                iterator.remove();
                return;
            }
        }
        list.add(ResourceLocation.decompose(itemId));
    }

    // ------------------------------------------------------------------
    // 权限（异步回包只写状态，不直接改界面）
    // ------------------------------------------------------------------

    private void requestPermission(Runnable onGranted) {
        var player = entrance.getClient().getPlayer();
        if (player == null) {
            status = tr("mcoo_build_with_ease.general_settings.no_player");
            return;
        }
        status = tr("mcoo_build_with_ease.general_settings.waiting");
        try {
            entrance.getChannel().sendPacket(new PlayerPermissionCheckPacket(player.getId()), packet -> {
                // 回包不在渲染线程上：切回客户端线程再改面板状态（旧界面也是这么做的）
                entrance.getClient().execute(() -> {
                    if (packet.granted()) {
                        granted = true;
                        status = null;
                        onGranted.run();
                    } else {
                        granted = false;
                        status = tr("mcoo_build_with_ease.general_settings.no_permission");
                    }
                });
            });
        } catch (Throwable error) {
            status = tr("mcoo_build_with_ease.general_settings.send_failed") + ": " + error.getClass().getSimpleName();
        }
    }

    // ------------------------------------------------------------------
    // 工具
    // ------------------------------------------------------------------

    private List<PlayerInfo> players() {
        var list = entrance.getClient().getOnlinePlayers();
        return list == null ? List.of() : list;
    }

    private static String tr(String key) {
        return Text.translate(key).getString();
    }

    private static String tr(String key, String arg) {
        return Text.translate(key, arg).getString();
    }

    private static boolean bool(Boolean value) {
        return Boolean.TRUE.equals(value);
    }

    private static int integer(Integer value) {
        return value == null ? 0 : value;
    }

    private static String value(Boolean flag) {
        return flag == null ? "默认" : (flag ? "是" : "否");
    }

    private static String num(Integer value) {
        return value == null ? "默认" : String.valueOf(value);
    }

    /** 可变的约束草稿：{@code ConstraintConfig} 是 record，12 个字段逐个重建太容易写错（老界面就写反过两个体积字段）。 */
    private static final class Edit {
        Boolean useCommands;
        Boolean allowUseMod;
        Boolean allowBreakBlocks;
        Boolean allowPlaceBlocks;
        Boolean allowInteractBlocks;
        Boolean allowCopyPasteStructures;
        Boolean useProperToolsOnly;
        Integer maxReachDistance;
        Integer maxBlockBreakVolume;
        Integer maxBlockPlaceVolume;
        Integer maxBlockInteractVolume;
        Integer maxStructureCopyPasteVolume;
        List<ResourceLocation> whitelistedItems;
        List<ResourceLocation> blacklistedItems;
        boolean targetListIsWhitelist;

        Edit(ConstraintConfig config) {
            if (config == null) {
                config = ConstraintConfig.DEFAULT;
            }
            this.useCommands = config.useCommands();
            this.allowUseMod = config.allowUseMod();
            this.allowBreakBlocks = config.allowBreakBlocks();
            this.allowPlaceBlocks = config.allowPlaceBlocks();
            this.allowInteractBlocks = config.allowInteractBlocks();
            this.allowCopyPasteStructures = config.allowCopyPasteStructures();
            this.useProperToolsOnly = config.useProperToolsOnly();
            this.maxReachDistance = config.maxReachDistance();
            this.maxBlockBreakVolume = config.maxBlockBreakVolume();
            this.maxBlockPlaceVolume = config.maxBlockPlaceVolume();
            this.maxBlockInteractVolume = config.maxBlockInteractVolume();
            this.maxStructureCopyPasteVolume = config.maxStructureCopyPasteVolume();
            this.whitelistedItems = config.whitelistedItems() == null ? null : new ArrayList<>(config.whitelistedItems());
            this.blacklistedItems = config.blacklistedItems() == null ? null : new ArrayList<>(config.blacklistedItems());
        }

        ConstraintConfig toConfig() {
            return new ConstraintConfig(
                    useCommands,
                    allowUseMod,
                    allowBreakBlocks,
                    allowPlaceBlocks,
                    allowInteractBlocks,
                    allowCopyPasteStructures,
                    useProperToolsOnly,
                    maxReachDistance,
                    maxBlockBreakVolume,
                    maxBlockPlaceVolume,
                    maxBlockInteractVolume,
                    maxStructureCopyPasteVolume,
                    whitelistedItems,
                    blacklistedItems);
        }
    }
}
