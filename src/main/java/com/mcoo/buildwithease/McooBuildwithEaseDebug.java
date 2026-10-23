package com.mcoo.buildwithease;

import com.mcoo.buildwithease.screen.builer.McooBuildwithEaseBuilderSettingsScreen;
import com.mcoo.buildwithease.screen.clipboard.McooBuildwithEaseClipboardScreen;
import com.mcoo.buildwithease.screen.clipboard.McooBuildwithEaseSaveFileScreen;
import com.mcoo.buildwithease.screen.general.McooBuildwithEaseGeneralSettingsScreen;
import com.mcoo.buildwithease.screen.pattern.McooBuildwithEasePatternScreen;
import com.mcoo.buildwithease.screen.settings.McooBuildwithEaseSessionStatusScreen;
import com.mcoo.buildwithease.screen.settings.McooBuildwithEaseSettingsScreen;
import com.mcoo.buildwithease.screen.structure.McooBuildwithEaseStructureScreen;
import com.mcoo.buildwithease.screen.transformer.McooBuildwithEaseTransformerPresetsSelectScreen;

/**
 * 调试辅助：用一个系统属性让客户端自动打开指定界面，方便无人值守地截图核对 GUI。
 * <p>
 * 用法：{@code -Dmcoo.debugScreen=settings|builder|clipboard}。
 * 正常游戏不会设置这个属性，因此不产生任何影响。
 */
final class McooBuildwithEaseDebug {

    private static final String PROPERTY = "mcoo.debugScreen";
    private static boolean opened = false;

    private McooBuildwithEaseDebug() {
    }

    static void openScreenIfRequested(McooBuildwithEaseClientManager manager) {
        if (opened) {
            return;
        }
        var screen = System.getProperty(PROPERTY);
        if (screen == null || screen.isBlank()) {
            return;
        }
        try {
            var entrance = McooBuildwithEaseClient.getInstance();
            // 剪贴板类的场景得先有一份内容才能预览。
            // 【为什么每帧重试】写剪贴板要求会话有效，而会话是连上服之后服务端才发过来的 ——
            // 头几帧往往还没生效，这时写进去会被丢掉，界面就只显示「剪贴板是空的」。
            // 所以先写、再看一眼写进去没有，没写进去就下一帧再来。
            var clipboard = buildDebugClipboard(entrance, screen);
            if (clipboard != null) {
                var player = entrance.getClient().getPlayer();
                if (player == null) {
                    return;
                }
                var builder = entrance.getStructureBuilder();
                builder.setClipboard(player, clipboard);
                if (builder.getContext(player).clipboard().isEmpty()) {
                    return;
                }
                new McooBuildwithEaseClipboardScreen(entrance).attach();
                opened = true;
                return;
            }
            switch (screen) {
                case "settings" -> new McooBuildwithEaseSettingsScreen(entrance).attach();
                case "builder" -> new McooBuildwithEaseBuilderSettingsScreen(entrance).attach();
                case "clipboard" -> new McooBuildwithEaseClipboardScreen(entrance).attach();
                case "radial" -> new McooBuildwithEaseStructureScreen(entrance, McooBuildwithEaseKeys.BUILD_MODE_RADIAL.getKeyBinding()).attach();
                case "general" -> new McooBuildwithEaseGeneralSettingsScreen(entrance).attach();
                case "pattern" -> {
                    // 图案默认是禁用的，禁用时只显示「启用/完成」两个按钮；
                    // 这里先打开，才能看到转换器列表和添加/编辑/删除那套 UI。
                    var builder = entrance.getStructureBuilder();
                    var player = entrance.getClient().getPlayer();
                    var pattern = builder.getContext(player).pattern();
                    if (!pattern.enabled()) {
                        builder.setPattern(player, pattern.toggled());
                    }
                    new McooBuildwithEasePatternScreen(entrance).attach();
                }
                case "presets" -> new McooBuildwithEaseTransformerPresetsSelectScreen(entrance, transformer -> {
                }).attach();
                case "session" -> new McooBuildwithEaseSessionStatusScreen(entrance).attach();
                case "savefile" -> new McooBuildwithEaseSaveFileScreen(entrance,
                        dev.huskuraft.universal.api.export.StructureExporter.Format.LITEMATIC,
                        "mcoo-test-structure.litematic",
                        3, 2, 3, java.util.List.of()).attach();
                default -> McooBuildwithEase.LOGGER.warn("Unknown debug screen: {}", screen);
            }
            opened = true;
        } catch (Throwable throwable) {
            McooBuildwithEase.LOGGER.error("Failed to open debug screen {}", screen, throwable);
        }
    }

    /**
     * 造一份假的剪贴板内容，用来验证结构预览（正常玩法要先进游戏捕捉）。
     * <p>
     * 返回 null 表示这个场景不是剪贴板预览。
     */
    private static com.mcoo.buildwithease.building.clipboard.Clipboard buildDebugClipboard(
            McooBuildwithEaseClient entrance, String screen) {
        var blocks = new java.util.ArrayList<com.mcoo.buildwithease.building.clipboard.BlockData>();
        switch (screen) {
            case "clipboard-full" -> {
                // 故意用好几种不同的方块：一来能看出贴图对不对，二来能看出面与面的明暗。
                var kinds = new dev.huskuraft.universal.api.core.Item[]{
                        dev.huskuraft.universal.api.core.Items.OAK_PLANKS.item(),
                        dev.huskuraft.universal.api.core.Items.BRICKS.item(),
                        dev.huskuraft.universal.api.core.Items.STONE_BRICKS.item(),
                        dev.huskuraft.universal.api.core.Items.GLASS.item(),
                        dev.huskuraft.universal.api.core.Items.SAND.item(),
                        dev.huskuraft.universal.api.core.Items.OAK_LOG.item()
                };
                for (int x = 0; x < 3; x++) {
                    for (int y = 0; y < 3; y++) {
                        for (int z = 0; z < 3; z++) {
                            blocks.add(new com.mcoo.buildwithease.building.clipboard.BlockData(
                                    new dev.huskuraft.universal.api.core.BlockPosition(x, y, z),
                                    kinds[(x * 3 + y * 2 + z) % kinds.length].getBlock().getDefaultBlockState(),
                                    null));
                        }
                    }
                }
                return clipboard("debug-full", blocks);
            }
            case "clipboard-fence" -> {
                // 栅栏专项测试：一排栅栏 + 楼梯。
                // 栅栏是典型的多部件方块（立柱 + 横杆），每个部件的 cullface 声明不同。
                var fence = dev.huskuraft.universal.api.core.Items.OAK_FENCE.item().getBlock().getDefaultBlockState();
                var stairs = dev.huskuraft.universal.api.core.Items.OAK_STAIRS.item().getBlock().getDefaultBlockState();
                for (int x = 0; x < 4; x++) {
                    for (int z = 0; z < 4; z++) {
                        // 相邻的栅栏/楼梯 —— 这样才会触发邻接关系
                        blocks.add(new com.mcoo.buildwithease.building.clipboard.BlockData(
                                new dev.huskuraft.universal.api.core.BlockPosition(x, 0, z),
                                (x + z) % 2 == 0 ? fence : stairs, null));
                    }
                }
                // 再叠一层，测试竖直方向相邻
                for (int x = 0; x < 4; x++) {
                    for (int z = 0; z < 4; z++) {
                        blocks.add(new com.mcoo.buildwithease.building.clipboard.BlockData(
                                new dev.huskuraft.universal.api.core.BlockPosition(x, 1, z),
                                fence, null));
                    }
                }
                return clipboard("debug-fence", blocks);
            }
            case "clipboard-entity" -> {
                // 方块实体专项测试：钟、箱子、陶罐、旗帜、信标、篝火、石头。
                // 这些方块的形状**不在方块状态模型里**（箱子/陶罐/旗帜的方块状态模型是空的），
                // 而是由各自的方块实体渲染器在 3D 管线里画 ——
                // 所以预览必须走 3D 渲染通道才能把它们画出来。
                var kinds = new dev.huskuraft.universal.api.core.Item[]{
                        dev.huskuraft.universal.api.core.Items.BELL.item(),
                        dev.huskuraft.universal.api.core.Items.CHEST.item(),
                        dev.huskuraft.universal.api.core.Items.DECORATED_POT.item(),
                        dev.huskuraft.universal.api.core.Items.WHITE_BANNER.item(),
                        dev.huskuraft.universal.api.core.Items.BEACON.item(),
                        dev.huskuraft.universal.api.core.Items.CAMPFIRE.item(),
                        dev.huskuraft.universal.api.core.Items.STONE.item()
                };
                for (int x = 0; x < kinds.length; x++) {
                    for (int y = 0; y < 2; y++) {
                        blocks.add(new com.mcoo.buildwithease.building.clipboard.BlockData(
                                new dev.huskuraft.universal.api.core.BlockPosition(x, y, 0),
                                kinds[x].getBlock().getDefaultBlockState(),
                                null));
                    }
                }
                return clipboard("debug-entity", blocks);
            }
            case "clipboard-snow" -> {
                // 雪片专项测试：石头地板 (y=0) + 上面一层雪片 (y=1)。
                // 雪片是薄层（1/8 高），上下/侧面的遮挡形状完全不对称。
                var stone = dev.huskuraft.universal.api.core.Items.STONE.item().getBlock().getDefaultBlockState();
                var snow = dev.huskuraft.universal.api.core.Items.SNOW.item().getBlock().getDefaultBlockState();
                for (int x = 0; x < 5; x++) {
                    for (int z = 0; z < 5; z++) {
                        blocks.add(new com.mcoo.buildwithease.building.clipboard.BlockData(
                                new dev.huskuraft.universal.api.core.BlockPosition(x, 0, z), stone, null));
                        blocks.add(new com.mcoo.buildwithease.building.clipboard.BlockData(
                                new dev.huskuraft.universal.api.core.BlockPosition(x, 1, z), snow, null));
                    }
                }
                return clipboard("debug-snow", blocks);
            }
            case "clipboard-partial" -> {
                // 不完整方块专项测试：雪片（薄层）、雪块、台阶、半砖、栅栏。
                // 这些方块的遮挡形状上下/侧面完全不对称。
                var kinds = new dev.huskuraft.universal.api.core.Item[]{
                        dev.huskuraft.universal.api.core.Items.SNOW.item(),
                        dev.huskuraft.universal.api.core.Items.SNOW_BLOCK.item(),
                        dev.huskuraft.universal.api.core.Items.OAK_STAIRS.item(),
                        dev.huskuraft.universal.api.core.Items.STONE_SLAB.item(),
                        dev.huskuraft.universal.api.core.Items.OAK_FENCE.item(),
                        dev.huskuraft.universal.api.core.Items.SAND.item()
                };
                for (int x = 0; x < 3; x++) {
                    for (int y = 0; y < 3; y++) {
                        for (int z = 0; z < 3; z++) {
                            blocks.add(new com.mcoo.buildwithease.building.clipboard.BlockData(
                                    new dev.huskuraft.universal.api.core.BlockPosition(x, y, z),
                                    kinds[(x * 3 + y * 2 + z) % kinds.length].getBlock().getDefaultBlockState(),
                                    null));
                        }
                    }
                }
                return clipboard("debug-partial", blocks);
            }
            case "clipboard-big" -> {
                // 大结构压力测试：实心 32x32x32 = 32768 块。
                // 用来验证大结构下预览还能不能流畅拖动。
                var state = dev.huskuraft.universal.api.core.Items.STONE.item().getBlock().getDefaultBlockState();
                for (int x = 0; x < 32; x++) {
                    for (int y = 0; y < 32; y++) {
                        for (int z = 0; z < 32; z++) {
                            blocks.add(new com.mcoo.buildwithease.building.clipboard.BlockData(
                                    new dev.huskuraft.universal.api.core.BlockPosition(x, y, z),
                                    state,
                                    null));
                        }
                    }
                }
                return clipboard("debug-big", blocks);
            }
            default -> {
                return null;
            }
        }
    }

    private static com.mcoo.buildwithease.building.clipboard.Clipboard clipboard(
            String name, java.util.List<com.mcoo.buildwithease.building.clipboard.BlockData> blocks) {
        return new com.mcoo.buildwithease.building.clipboard.Clipboard(
                true, new com.mcoo.buildwithease.building.clipboard.Snapshot(name, 0, blocks));
    }

}
