package dev.huskuraft.universal.vanilla.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.textures.GpuTextureView;

import cn.enaium.fabric.imgui.TextureBindings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;

/**
 * 物品注册表的枚举 + 物品图标（给 ImGui 的物品选择页用）。
 *
 * <h3>为什么不走创造模式搜索树</h3>
 * {@code ClientContentFactory#searchItemStack} 背后是创造型标签页的搜索树：
 * ① 空查询返回空（没法「列出全部」）；② 它要拿 {@code player.connection}，玩家没进世界时会 NPE。
 * 物品选择页要的是「注册表里有什么」，与玩家状态无关 —— 所以直接读注册表。
 *
 * <h3>图标为什么不能靠「猜贴图路径」</h3>
 * 一开始按 {@code item/<name>} / {@code block/<name>} 去图集里找，结果一半物品找不到：
 * 物品图标用的贴图是由<b>模型</b>决定的（模型 JSON 里 {@code textures.layer0} 指向哪张就是哪张），
 * 路径和物品 id 经常对不上（刷怪蛋、药水、弓、船……全是特例）。
 *
 * <p>正确做法是问 MC 自己：把物品塞进 {@link ItemStackRenderState}（GUI 上下文），
 * 取它的「粒子贴图」——{@code pickParticleMaterial} 给的就是图标用的那张精灵。
 * 精灵自带 {@code atlasLocation()}，所以还知道它属于哪本图集。
 *
 * <h3>缓存</h3>
 * 解析一次不便宜（要过模型系统），所以按物品 id 缓存<b>UV + 所属图集的 view</b>。
 * <b>不缓存 ImGui 的 textureId</b>：宿主每帧 {@code clearStale()} 回收 id，跨帧用就是野指针；
 * id 每次现算（哈希查表，很便宜）。
 */
public final class McooImGuiItems {

    private static volatile List<String> allIds;

    /** 缓存项：UV + 精灵所属图集的 view（view 关了说明资源重载过，要重算）。 */
    private record Icon(float u0, float v0, float u1, float v1, GpuTextureView view) {
    }

    private static final Map<String, Icon> ICONS = new HashMap<>();

    private McooImGuiItems() {
    }

    /** 全部物品 id（{@code namespace:path}），按 id 排序。 */
    public static List<String> allItemIds() {
        var list = allIds;
        if (list != null) {
            return list;
        }
        var built = new ArrayList<String>(1024);
        for (Identifier id : BuiltInRegistries.ITEM.keySet()) {
            built.add(id.getNamespace() + ":" + id.getPath());
        }
        built.sort(String::compareTo);
        list = List.copyOf(built);
        allIds = list;
        return list;
    }

    /** 物品显示名（走 MC 悬停名，自动跟语言文件）。拿不到退回 id。 */
    public static String displayName(String itemId) {
        var id = Identifier.tryParse(itemId);
        if (id == null) {
            return itemId;
        }
        var item = BuiltInRegistries.ITEM.getValue(id);
        if (item == null) {
            return itemId;
        }
        try {
            return new net.minecraft.world.item.ItemStack(item).getHoverName().getString();
        } catch (Throwable error) {
            return itemId;
        }
    }

    /** 物品图标的 UV：{@code {u0,v0,u1,v1}}；解析不出返回 {@code null}。 */
    public static float[] iconUv(String itemId) {
        var atlas = McooItemIconAtlas.get();
        return atlas == null ? null : atlas.uvFor(itemId);
    }

    /**
     * 图标图集的 ImGui 纹理 id（<b>每帧现取</b>）；图集还没建好返回 {@code 0}。
     *
     * <p>参数留着是为了调用点好读（所有物品共用同一张图集纹理，不再按物品区分）。
     */
    public static long iconTextureId(String itemId) {
        var atlas = McooItemIconAtlas.get();
        return atlas == null ? 0L : atlas.textureId();
    }

    private static Icon icon(String itemId) {
        var id = Identifier.tryParse(itemId);
        if (id == null) {
            return null;
        }
        var cached = ICONS.get(itemId);
        if (cached != null && cached.view() != null && !cached.view().isClosed()) {
            return cached;
        }
        try {
            var item = BuiltInRegistries.ITEM.getValue(id);
            if (item == null) {
                return null;
            }
            var minecraft = Minecraft.getInstance();
            // GUI 上下文 + 固定种子：图标解析必须稳定
            var state = new ItemStackRenderState();
            minecraft.getItemModelResolver().updateForTopItem(
                    state,
                    new net.minecraft.world.item.ItemStack(item),
                    ItemDisplayContext.GUI,
                    minecraft.level,
                    null,
                    0);
            var material = state.pickParticleMaterial(RandomSource.create(42L));
            if (material == null) {
                return null;
            }
            var sprite = material.sprite();
            var contents = sprite.contents();
            var atlas = atlas(sprite.atlasLocation());
            if (atlas == null || contents == null) {
                return null;
            }
            var missing = atlas.missingSprite().contents();
            if (missing != null && contents.name().equals(missing.name())) {
                return null;
            }
            var icon = new Icon(sprite.getU0(), sprite.getV0(), sprite.getU1(), sprite.getV1(),
                    atlas.getTextureView());
            ICONS.put(itemId, icon);
            return icon;
        } catch (Throwable error) {
            // 资源重载窗口期模型系统会抛：当作「暂时没图标」，下一帧重算，不缓存失败结果
            return null;
        }
    }

    private static TextureAtlas atlas(Identifier location) {
        try {
            var texture = Minecraft.getInstance().getTextureManager().getTexture(location);
            return texture instanceof TextureAtlas textureAtlas ? textureAtlas : null;
        } catch (Throwable error) {
            return null;
        }
    }

    /** 资源重载后清缓存（图集换了，UV 全部失效）。 */
    public static void clearCache() {
        ICONS.clear();
        allIds = null;
    }

}
