package com.mcoo.buildwithease;

import com.mcoo.buildwithease.screen.builer.McooBuildwithEaseBuilderSettingsScreen;
import com.mcoo.buildwithease.screen.clipboard.McooBuildwithEaseClipboardScreen;
import com.mcoo.buildwithease.screen.clipboard.McooBuildwithEaseSaveFileScreen;
import com.mcoo.buildwithease.screen.general.McooBuildwithEaseGeneralSettingsScreen;
import com.mcoo.buildwithease.screen.imgui.McooBuildwithEaseRenderSettingsImGuiScreen;
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
                var clipboardScreen = new McooBuildwithEaseClipboardScreen(entrance);
                clipboardScreen.attach();
                // 「导出图片」场景：等预览先渲染几帧（渲染器这时才拿到），再自动导一次
                if ("clipboard-export".equals(screen)) {
                    clipboardScreen.getSnapshotWidget().requestExport(1024, 683);
                } else if ("clipboard-export4k".equals(screen)) {
                    clipboardScreen.getSnapshotWidget().requestExport(4096, 2731);
                } else if ("clipboard-history".equals(screen)) {
                    // 快照历史专项测试：把这份快照塞进历史，然后直接打开历史列表
                    entrance.getConfigStorage().update(config -> config
                            .withClipboardConfig(config.clipboardConfig().appendHistory(clipboard.snapshot())));
                    new com.mcoo.buildwithease.screen.clipboard.McooBuildwithEaseStructureSnapshotsHistoryScreen(
                            entrance, snapshot -> {
                    }).attach();
                }
                opened = true;
                return;
            }
            switch (screen) {
                case "settings" -> new McooBuildwithEaseSettingsScreen(entrance).attach();
                // ImGui 版渲染设置（13 个模块 / 58 个参数）—— 用来无人值守截图核对
                case "render" -> new McooBuildwithEaseRenderSettingsImGuiScreen(entrance).attach();
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
            case "clipboard-water" -> {
                // 含水方块专项测试：水不是方块模型的一部分（原版由流体渲染器单独画），
                // 所以水方块本身在预览里是「空的」，含水的树叶/楼梯则是「方块模型 + 水」两层。
                var builder = entrance.getStructureBuilder();
                var player = entrance.getClient().getPlayer();
                var factory = dev.huskuraft.universal.api.platform.ContentFactory.getInstance();
                var water = factory.newOptionalBlock(dev.huskuraft.universal.api.core.ResourceLocation.vanilla("water"))
                        .map(block -> block.getDefaultBlockState()).orElse(null);
                var lava = factory.newOptionalBlock(dev.huskuraft.universal.api.core.ResourceLocation.vanilla("lava"))
                        .map(block -> block.getDefaultBlockState()).orElse(null);
                var wetLeaves = withWaterlogged(
                        dev.huskuraft.universal.api.core.Items.OAK_LEAVES.item().getBlock().getDefaultBlockState());
                var wetStairs = withWaterlogged(
                        dev.huskuraft.universal.api.core.Items.OAK_STAIRS.item().getBlock().getDefaultBlockState());
                var column = 0;
                if (water != null) {
                    blocks.add(new com.mcoo.buildwithease.building.clipboard.BlockData(
                            new dev.huskuraft.universal.api.core.BlockPosition(column++, 0, 0), water, null));
                }
                if (lava != null) {
                    blocks.add(new com.mcoo.buildwithease.building.clipboard.BlockData(
                            new dev.huskuraft.universal.api.core.BlockPosition(column++, 0, 0), lava, null));
                }
                blocks.add(new com.mcoo.buildwithease.building.clipboard.BlockData(
                        new dev.huskuraft.universal.api.core.BlockPosition(column++, 0, 0), wetLeaves, null));
                blocks.add(new com.mcoo.buildwithease.building.clipboard.BlockData(
                        new dev.huskuraft.universal.api.core.BlockPosition(column, 0, 0), wetStairs, null));
                return clipboard("debug-water", blocks);
            }
            case "clipboard-export", "clipboard-export4k" -> {
                // 导出图片专项测试：铺一片方块，界面打开后自动导一张透明 PNG。
                // clipboard-export4k 用来测 4K 大图（顺带测「物理尺寸 ≠ 请求尺寸」的裁切路径）。
                var exportKinds = new dev.huskuraft.universal.api.core.Item[]{
                        dev.huskuraft.universal.api.core.Items.OAK_PLANKS.item(),
                        dev.huskuraft.universal.api.core.Items.BRICKS.item(),
                        dev.huskuraft.universal.api.core.Items.STONE_BRICKS.item(),
                        dev.huskuraft.universal.api.core.Items.GLASS.item(),
                        dev.huskuraft.universal.api.core.Items.SAND.item(),
                        dev.huskuraft.universal.api.core.Items.OAK_LOG.item()
                };
                for (int x = 0; x < 4; x++) {
                    for (int y = 0; y < 3; y++) {
                        for (int z = 0; z < 4; z++) {
                            blocks.add(new com.mcoo.buildwithease.building.clipboard.BlockData(
                                    new dev.huskuraft.universal.api.core.BlockPosition(x, y, z),
                                    exportKinds[(x * 3 + y * 2 + z) % exportKinds.length].getBlock().getDefaultBlockState(),
                                    null));
                        }
                    }
                }
                return clipboard("debug-export", blocks);
            }
            case "clipboard-light" -> {
                // 发光方块专项测试：萤石、海晶灯、灯笼、荧石灯这些「自带光」的方块，
                // 预览里应该明显比普通方块亮（之前一律用满亮度，画面很平）。
                var kinds = new dev.huskuraft.universal.api.core.Item[]{
                        dev.huskuraft.universal.api.core.Items.GLOWSTONE.item(),
                        dev.huskuraft.universal.api.core.Items.SEA_LANTERN.item(),
                        dev.huskuraft.universal.api.core.Items.SHROOMLIGHT.item(),
                        dev.huskuraft.universal.api.core.Items.LANTERN.item(),
                        dev.huskuraft.universal.api.core.Items.REDSTONE_LAMP.item(),
                        dev.huskuraft.universal.api.core.Items.BEACON.item(),
                        dev.huskuraft.universal.api.core.Items.STONE.item(),
                        dev.huskuraft.universal.api.core.Items.OAK_PLANKS.item(),
                        dev.huskuraft.universal.api.core.Items.GLASS.item()
                };
                for (int x = 0; x < 3; x++) {
                    for (int y = 0; y < 2; y++) {
                        for (int z = 0; z < 3; z++) {
                            blocks.add(new com.mcoo.buildwithease.building.clipboard.BlockData(
                                    new dev.huskuraft.universal.api.core.BlockPosition(x, y, z),
                                    kinds[(x * 3 + y * 2 + z) % kinds.length].getBlock().getDefaultBlockState(),
                                    null));
                        }
                    }
                }
                return clipboard("debug-light", blocks);
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
            case "clipboard-history" -> {
                // 快照历史专项测试：方块种类多 → 历史列表里的图标网格很高。
                // 以前条目高度算不对，下面的图标会被裁掉，列表也滚不到底。
                var kinds = new dev.huskuraft.universal.api.core.Item[]{
                        dev.huskuraft.universal.api.core.Items.STONE.item(),
                        dev.huskuraft.universal.api.core.Items.COBBLESTONE.item(),
                        dev.huskuraft.universal.api.core.Items.MOSSY_COBBLESTONE.item(),
                        dev.huskuraft.universal.api.core.Items.STONE_BRICKS.item(),
                        dev.huskuraft.universal.api.core.Items.MOSSY_STONE_BRICKS.item(),
                        dev.huskuraft.universal.api.core.Items.CRACKED_STONE_BRICKS.item(),
                        dev.huskuraft.universal.api.core.Items.CHISELED_STONE_BRICKS.item(),
                        dev.huskuraft.universal.api.core.Items.GRAVEL.item(),
                        dev.huskuraft.universal.api.core.Items.SAND.item(),
                        dev.huskuraft.universal.api.core.Items.SANDSTONE.item(),
                        dev.huskuraft.universal.api.core.Items.CHISELED_SANDSTONE.item(),
                        dev.huskuraft.universal.api.core.Items.CUT_SANDSTONE.item(),
                        dev.huskuraft.universal.api.core.Items.SMOOTH_SANDSTONE.item(),
                        dev.huskuraft.universal.api.core.Items.RED_SAND.item(),
                        dev.huskuraft.universal.api.core.Items.RED_SANDSTONE.item(),
                        dev.huskuraft.universal.api.core.Items.COAL_ORE.item(),
                        dev.huskuraft.universal.api.core.Items.IRON_ORE.item(),
                        dev.huskuraft.universal.api.core.Items.OAK_PLANKS.item(),
                        dev.huskuraft.universal.api.core.Items.BRICKS.item(),
                        dev.huskuraft.universal.api.core.Items.GLASS.item(),
                        dev.huskuraft.universal.api.core.Items.OAK_LOG.item(),
                        dev.huskuraft.universal.api.core.Items.OAK_LEAVES.item(),
                        dev.huskuraft.universal.api.core.Items.OAK_SLAB.item(),
                        dev.huskuraft.universal.api.core.Items.OAK_STAIRS.item(),
                        dev.huskuraft.universal.api.core.Items.WHITE_WOOL.item(),
                        dev.huskuraft.universal.api.core.Items.BLACK_WOOL.item(),
                        dev.huskuraft.universal.api.core.Items.ICE.item(),
                        dev.huskuraft.universal.api.core.Items.SNOW_BLOCK.item(),
                        dev.huskuraft.universal.api.core.Items.SNOW.item(),
                        dev.huskuraft.universal.api.core.Items.CLAY.item(),
                        dev.huskuraft.universal.api.core.Items.BRICKS.item()
                };
                var column = 0;
                for (var kind : kinds) {
                    for (int i = 0; i < 4; i++) {
                        blocks.add(new com.mcoo.buildwithease.building.clipboard.BlockData(
                                new dev.huskuraft.universal.api.core.BlockPosition(column++, 0, 0),
                                kind.getBlock().getDefaultBlockState(),
                                null));
                    }
                }
                return clipboard("debug-history", blocks);
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

    /** 把方块状态改成「含水」。 */
    private static dev.huskuraft.universal.api.core.BlockState withWaterlogged(
            dev.huskuraft.universal.api.core.BlockState state) {
        var property = state.getPropertiesMap().keySet().stream()
                .filter(p -> "waterlogged".equals(p.getName()))
                .findFirst().orElse(null);
        if (property == null) {
            return state;
        }
        var value = property.getValue("true").orElse(null);
        return value == null ? state : state.withProperty(property, value);
    }

}
