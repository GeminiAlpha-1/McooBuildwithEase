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
        opened = true;
        try {
            var entrance = McooBuildwithEaseClient.getInstance();
            switch (screen) {
                case "settings" -> new McooBuildwithEaseSettingsScreen(entrance).attach();
                case "builder" -> new McooBuildwithEaseBuilderSettingsScreen(entrance).attach();
                case "clipboard" -> new McooBuildwithEaseClipboardScreen(entrance).attach();
                case "clipboard-full" -> {
                    // 塞一份假的剪贴板内容进去，好验证结构预览（正常玩法要先进游戏捕捉）。
                    // 故意用好几种不同的方块：一来能看出贴图对不对，二来能看出面与面的明暗。
                    var builder = entrance.getStructureBuilder();
                    var player = entrance.getClient().getPlayer();
                    var blocks = new java.util.ArrayList<com.mcoo.buildwithease.building.clipboard.BlockData>();
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
                    builder.setClipboard(player, new com.mcoo.buildwithease.building.clipboard.Clipboard(
                            true, new com.mcoo.buildwithease.building.clipboard.Snapshot("debug", 0, blocks)));
                    new McooBuildwithEaseClipboardScreen(entrance).attach();
                }
                case "clipboard-fence" -> {
                    // 栅栏专项测试：一排栅栏 + 楼梯。
                    // 栅栏是典型的多部件方块（立柱 + 横杆），每个部件的 cullface 声明不同，
                    // 剔除逻辑一旦用错就会只剩顶面。
                    var builder = entrance.getStructureBuilder();
                    var player = entrance.getClient().getPlayer();
                    var blocks = new java.util.ArrayList<com.mcoo.buildwithease.building.clipboard.BlockData>();
                    var fence = dev.huskuraft.universal.api.core.Items.OAK_FENCE.item().getBlock().getDefaultBlockState();
                    var stairs = dev.huskuraft.universal.api.core.Items.OAK_STAIRS.item().getBlock().getDefaultBlockState();
                    for (int x = 0; x < 4; x++) {
                        for (int z = 0; z < 4; z++) {
                            // 相邻的栅栏/楼梯 —— 这样才会触发邻接剔除，
                            // 才能验证「相邻时侧面还在不在」。
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
                    builder.setClipboard(player, new com.mcoo.buildwithease.building.clipboard.Clipboard(
                            true, new com.mcoo.buildwithease.building.clipboard.Snapshot("debug-fence", 0, blocks)));
                    new McooBuildwithEaseClipboardScreen(entrance).attach();
                }
                case "clipboard-snow" -> {
                    // 雪片专项测试：石头地板 (y=0) + 上面一层雪片 (y=1)。
                    // 雪片是薄层（1/8 高），上下/侧面的遮挡形状完全不对称 ——
                    // 邻接剔除如果用错方向，雪片的顶面会被误剔，地板上就出现一个个洞。
                    var builder = entrance.getStructureBuilder();
                    var player = entrance.getClient().getPlayer();
                    var blocks = new java.util.ArrayList<com.mcoo.buildwithease.building.clipboard.BlockData>();
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
                    builder.setClipboard(player, new com.mcoo.buildwithease.building.clipboard.Clipboard(
                            true, new com.mcoo.buildwithease.building.clipboard.Snapshot("debug-snow", 0, blocks)));
                    new McooBuildwithEaseClipboardScreen(entrance).attach();
                }
                case "clipboard-partial" -> {
                    // 不完整方块专项测试：雪片（薄层）、雪块、台阶、半砖、栅栏。
                    // 这些方块的遮挡形状上下/侧面完全不对称 —— 邻接剔除如果用错方向，
                    // 会把玩家看得见的面剔掉（表现为结构上出现一个个「洞」）。
                    var builder = entrance.getStructureBuilder();
                    var player = entrance.getClient().getPlayer();
                    var blocks = new java.util.ArrayList<com.mcoo.buildwithease.building.clipboard.BlockData>();
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
                    builder.setClipboard(player, new com.mcoo.buildwithease.building.clipboard.Clipboard(
                            true, new com.mcoo.buildwithease.building.clipboard.Snapshot("debug-partial", 0, blocks)));
                    new McooBuildwithEaseClipboardScreen(entrance).attach();
                }
                case "clipboard-big" -> {
                    // 大结构压力测试：实心 32x32x32 = 32768 块。
                    // 实心结构的内部面全被相邻方块挡住，邻接剔除后只剩表面 6144 个面。
                    // 用来验证剔除是否真的生效、以及大结构下预览还能不能流畅拖动。
                    var builder = entrance.getStructureBuilder();
                    var player = entrance.getClient().getPlayer();
                    var blocks = new java.util.ArrayList<com.mcoo.buildwithease.building.clipboard.BlockData>();
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
                    builder.setClipboard(player, new com.mcoo.buildwithease.building.clipboard.Clipboard(
                            true, new com.mcoo.buildwithease.building.clipboard.Snapshot("debug-big", 0, blocks)));
                    new McooBuildwithEaseClipboardScreen(entrance).attach();
                }
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
        } catch (Throwable throwable) {
            McooBuildwithEase.LOGGER.error("Failed to open debug screen {}", screen, throwable);
        }
    }

}
