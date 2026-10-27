package com.mcoo.buildwithease.screen.imgui.panel;

import com.mcoo.buildwithease.McooBuildwithEaseClient;
import com.mcoo.buildwithease.screen.imgui.widget.ImGuiShell;
import com.mcoo.buildwithease.screen.imgui.widget.ImGuiWidgets;
import com.mcoo.buildwithease.session.config.ConstraintConfig;

import dev.huskuraft.universal.api.core.PlayerInfo;

import java.util.List;

/**
 * 玩家与会话面板：<b>中间</b>在线玩家列表，<b>右侧</b>会话状态 + 权限摘要。
 *
 * <h3>为什么把「玩家列表」和「通用设置」放一起</h3>
 * 服务端那套限制配置是<b>按玩家覆盖全局</b>的结构（{@code ConstraintConfig} 全局一份 +
 * 每个玩家可选覆盖）。所以「选中一个玩家 → 看他受什么限制」天然就是两栏形态，
 * 不需要再单独做一个「通用设置」页面 —— 全局值直接显示在右栏上半部分。
 *
 * <h3>这一版是只读</h3>
 * 写配置要过服务端的权限校验（{@code PlayerPermissionCheckPacket} + 会话有效），
 * 是异步往返。先把「看」做对，改的流程单独做一版，不在只读界面上挂半残的写入口。
 */
public final class PlayerListPanel implements ImGuiPanel {

    private static final String ID = "players";

    private final McooBuildwithEaseClient entrance;

    /** 权限申请的状态文案（null = 还没申请过）。异步往返的结果写在这里，不直接改界面。 */
    private String permissionStatus;

    /** 选中的玩家 uuid（null = 默认第一个）。 */
    private java.util.UUID selectedId;

    public PlayerListPanel(McooBuildwithEaseClient entrance) {
        this.entrance = entrance;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String titleKey() {
        return "mcoo_build_with_ease.player_list.title";
    }

    // ------------------------------------------------------------------
    // 中间栏：在线玩家
    // ------------------------------------------------------------------

    @Override
    public void renderContent() {
        var players = players();
        if (players.isEmpty()) {
            ImGuiWidgets.diagnostic("(no players)");
            return;
        }
        if (selectedId == null) {
            selectedId = players.get(0).getProfile().getId();
        }

        for (var player : players) {
            var id = player.getProfile().getId();
            boolean selected = id.equals(selectedId);
            // 名字前面加个圆点：在线玩家列表里"当前选中"和"名字"是两回事，别混
            String label = (selected ? "● " : "○ ") + nameOf(player);
            if (ImGuiShell.navItemRaw(label, selected)) {
                selectedId = id;
            }
        }
    }

    // ------------------------------------------------------------------
    // 右栏：会话状态 + 约束摘要
    // ------------------------------------------------------------------

    @Override
    public void renderDetails() {
        ImGuiShell.panelTitle("mcoo_build_with_ease.shell.details");

        var sessionManager = entrance.getSessionManager();
        boolean valid = sessionManager.isSessionValid();
        ImGuiShell.fieldText("mcoo_build_with_ease.player_list.session",
                valid ? "有效" : "未连接");
        ImGuiShell.fieldText("mcoo_build_with_ease.player_list.online",
                String.valueOf(players().size()));

        var config = valid
                ? sessionManager.getServerSessionConfigOrEmpty()
                : null;
        if (config == null) {
            ImGuiWidgets.separator();
            ImGuiWidgets.text("mcoo_build_with_ease.player_list.no_session");
            return;
        }

        ImGuiWidgets.separator();
        // 改配置要过服务端权限校验（异步往返）：点一下发请求，结果写进 permissionStatus。
        // 没有权限时服务端不会回包内容，按钮这一侧只负责"申请"，不假设自己一定能改。
        if (ImGuiWidgets.button("mcoo_build_with_ease.player_list.request_permission")) {
            requestPermission();
        }
        if (permissionStatus != null) {
            ImGuiWidgets.diagnostic(permissionStatus);
        }

        ImGuiWidgets.separator();
        ImGuiWidgets.diagnostic("全局限制（只读）");
        renderConstraints(config.getGlobalConfig());

        var selected = selectedPlayer();
        if (selected == null) {
            return;
        }
        var override = config.playerConfigs().get(selected.getProfile().getId());
        ImGuiWidgets.separator();
        ImGuiWidgets.diagnostic("玩家覆盖：" + nameOf(selected));
        if (override == null) {
            ImGuiWidgets.diagnostic("(沿用全局)");
        } else {
            renderConstraints(override);
        }
    }

    /**
     * 向服务端申请「能否修改设置」。
     * <p>
     * ⚠️ 这是<b>异步</b>的：回调在另一个时机执行，所以只把结果写进状态字段，
     * 绝不在回调里直接操作 ImGui 状态（ImGui 是单线程逐帧的）。
     */
    private void requestPermission() {
        var player = entrance.getClient().getPlayer();
        if (player == null) {
            permissionStatus = "没有玩家上下文";
            return;
        }
        permissionStatus = "等待服务端回应……";
        try {
            entrance.getChannel().sendPacket(
                    new com.mcoo.buildwithease.networking.packets.player.PlayerPermissionCheckPacket(player.getId()),
                    packet -> permissionStatus = packet.granted() ? "有权限，可修改" : "无权限（需要 OP）");
        } catch (Throwable error) {
            permissionStatus = "发送失败：" + error.getClass().getSimpleName();
        }
    }

    /** 约束摘在一块画；null = 沿用全局（{@code ConstraintConfig} 的字段是装箱类型）。 */
    private static void renderConstraints(ConstraintConfig config) {
        if (config == null) {
            ImGuiWidgets.diagnostic("(沿用全局)");
            return;
        }
        ImGuiWidgets.diagnostic("允许使用模组 = " + value(config.allowUseMod()));
        ImGuiWidgets.diagnostic("允许破坏方块 = " + value(config.allowBreakBlocks()));
        ImGuiWidgets.diagnostic("允许放置方块 = " + value(config.allowPlaceBlocks()));
        ImGuiWidgets.diagnostic("仅限合适工具 = " + value(config.useProperToolsOnly()));
        ImGuiWidgets.diagnostic("最大触及距离 = " + num(config.maxReachDistance()));
        ImGuiWidgets.diagnostic("最大破坏体积 = " + num(config.maxBlockBreakVolume()));
        ImGuiWidgets.diagnostic("最大放置体积 = " + num(config.maxBlockPlaceVolume()));
        ImGuiWidgets.diagnostic("最大复制体积 = " + num(config.maxStructureCopyPasteVolume()));
    }

    private static String value(Boolean flag) {
        return flag == null ? "默认" : (flag ? "是" : "否");
    }

    private static String num(Integer value) {
        return value == null ? "默认" : String.valueOf(value);
    }

    /**
     * 玩家显示名。
     * <p>
     * ⚠️ {@code PlayerInfo#getDisplayName()} 在部分实现里是 <b>null</b>（进服早期/离线信息未同步），
     * 直接 {@code getString()} 会 NPE —— 踩过：整栏变成 "panel content failed"。
     * 所以一律回退到 profile 里的名字。
     */
    private static String nameOf(PlayerInfo player) {
        var display = player.getDisplayName();
        if (display != null) {
            return display.getString();
        }
        var profile = player.getProfile();
        return profile == null ? "?" : profile.getName();
    }

    private List<PlayerInfo> players() {
        var list = entrance.getClient().getOnlinePlayers();
        return list == null ? List.of() : list;
    }

    private PlayerInfo selectedPlayer() {
        for (var player : players()) {
            if (player.getProfile().getId().equals(selectedId)) {
                return player;
            }
        }
        return players().isEmpty() ? null : players().get(0);
    }

}
