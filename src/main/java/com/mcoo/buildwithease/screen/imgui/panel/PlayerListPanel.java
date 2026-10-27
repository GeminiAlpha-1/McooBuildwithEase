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
