package com.mcoo.buildwithease.screen.imgui;

import com.mcoo.buildwithease.McooBuildwithEaseClient;
import com.mcoo.buildwithease.screen.common.McooBuildwithEaseScreen;
import com.mcoo.buildwithease.screen.imgui.widget.ImGuiWidgets;
import dev.huskuraft.universal.api.gui.ImGuiScreen;
import dev.huskuraft.universal.api.text.Text;
import dev.huskuraft.universal.vanilla.gui.McooImGuiTextures;

/**
 * <b>纹理喂入 spike</b>：验证"把 MC 纹理画进 ImGui"这条路能不能走通。
 *
 * <p>要回答的问题：ImGui 界面里能不能显示方块/物品图标？
 * 这决定物品选择页 / 图案列表 / 剪贴板面板能不能在 ImGui 里做 ——
 * 做不了的话，这些界面就继续留在自研 GUI 里。
 *
 * <p>验证分两步（同一个界面里都能看到）：
 * <ol>
 *     <li><b>整张方块图集</b>：把图集作为一个纹理整体缩放画出来 —— 证明纹理绑定本身通了；</li>
 *     <li><b>按 UV 裁出的方块图标</b>：证明"图标网格"可行（图标不额外上传纹理，只换 UV）。</li>
 * </ol>
 *
 * <p>调出来看：{@code -Dmcoo.debugScreen=texspike}。
 *
 * <p>⚠️ 这是验证用的临时界面。结论确认后：通过就把 {@link ImGuiWidgets#image} 收进正式控件层
 * （已经在了），本类可以删；不通过就把这批界面继续留在自研 GUI。
 */
public class McooBuildwithEaseTextureSpikeScreen extends McooBuildwithEaseScreen implements ImGuiScreen {

    /** 拿来试的几个方块 sprite（都是方块图集里的常用条目）。 */
    private static final String[] SPRITES = {
            "minecraft:block/stone",
            "minecraft:block/oak_planks",
            "minecraft:block/bricks",
            "minecraft:block/glass",
            "minecraft:block/glowstone",
            "minecraft:block/diamond_ore",
            "minecraft:block/cobblestone",
            "minecraft:block/mossy_cobblestone",
            "minecraft:block/oak_log",
            "minecraft:block/oak_leaves",
            "minecraft:block/sand",
            "minecraft:block/sandstone",
            "minecraft:block/white_wool",
            "minecraft:block/ice",
            "minecraft:block/iron_ore",
            "minecraft:block/clay",
            "minecraft:block/netherrack",
            "minecraft:block/obsidian",
    };

    private static final float ICON_SIZE = 28f;
    private static final int ICON_COLUMNS = 9;
    private static final float ATLAS_PREVIEW = 96f;

    public McooBuildwithEaseTextureSpikeScreen(McooBuildwithEaseClient entrance) {
        super(entrance, Text.translate("mcoo_build_with_ease.shell.tools"));
    }

    @Override
    public void renderImGui() {
        if (!ImGuiWidgets.beginWindow("mcoo_build_with_ease.shell.tools", 560f, 460f)) {
            ImGuiWidgets.endWindow();
            return;
        }

        long atlasId = McooImGuiTextures.blockAtlasId();
        ImGuiWidgets.diagnostic("blocks atlas textureId = " + atlasId
                + "  ready = " + McooImGuiTextures.isBlockAtlasReady());
        for (String line : McooImGuiTextures.diagnostics().split("\n")) {
            ImGuiWidgets.diagnostic("  " + line);
        }

        ImGuiWidgets.separator();

        // ② 等轴测图标：三个四边形拼出「立方体」观感（不需要 FBO）
        ImGuiWidgets.diagnostic("2) isometric icons (3 quads per block, no FBO)");
        for (int i = 0; i < SPRITES.length; i++) {
            if (i % ICON_COLUMNS != 0) {
                imgui.ImGui.sameLine(0f, 4f);
            }
            float[] uv = McooImGuiTextures.blockSpriteUv(SPRITES[i]);
            ImGuiWidgets.blockIcon(atlasId, uv, ICON_SIZE);
        }

        ImGuiWidgets.separator();

        // ③ 按 UV 裁出的平面图标：和上面的等轴测版对照
        ImGuiWidgets.diagnostic("3) flat icons cropped by UV (for comparison)");
        for (int i = 0; i < SPRITES.length; i++) {
            if (i % ICON_COLUMNS != 0) {
                imgui.ImGui.sameLine(0f, 4f);
            }
            float[] uv = McooImGuiTextures.blockSpriteUv(SPRITES[i]);
            ImGuiWidgets.image(atlasId, ICON_SIZE, ICON_SIZE, uv);
        }

        ImGuiWidgets.separator();

        // ① 整张图集（缩略）：能看见方块贴图 = 纹理绑定通了
        ImGuiWidgets.diagnostic("1) whole blocks atlas (scaled down)");
        ImGuiWidgets.image(atlasId, ATLAS_PREVIEW, ATLAS_PREVIEW, null);

        ImGuiWidgets.separator();
        float[] first = McooImGuiTextures.blockSpriteUv(SPRITES[0]);
        ImGuiWidgets.diagnostic("uv sample: " + SPRITES[0] + " -> " + (first == null
                ? "null"
                : String.format("%.4f, %.4f, %.4f, %.4f", first[0], first[1], first[2], first[3])));

        ImGuiWidgets.endWindow();
    }

}
