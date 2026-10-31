package dev.huskuraft.universal.vanilla.gui;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;

import org.joml.Vector4f;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.PoseStack;

import cn.enaium.fabric.imgui.TextureBindings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;

/**
 * 物品图标的<b>离屏 3D 烘焙图集</b>：把物品模型真渲染进一张大纹理的槽位里，给 ImGui 当图标用。
 *
 * <h3>为什么不用 {@code pickParticleMaterial()}</h3>
 * 那是物品的<b>粒子贴图</b>（物品破坏时飘的碎屑用的那张平面图），不是物品模型。
 * 拿它当图标，方块全是"贴图压扁的一块"，和原版创造物品栏的立体图标完全不是一回事。
 * 原版创造栏的图标是<b>真 3D</b> —— 走 {@code GuiItemAtlas}（本类就是照着它实现的）。
 *
 * <h3>烘焙配方（逐条对照 26.2 的 {@code GuiItemAtlas#drawToSlot} 字节码）</h3>
 * <ol>
 *   <li>{@code poseStack.translate(px + slot/2, py + slot/2, 0)} 把原点挪到槽位中心；</li>
 *   <li>{@code poseStack.scale(slot, -slot, slot)} —— <b>Y 取负</b>，GUI 坐标系要翻；</li>
 *   <li>{@code RenderSystem.outputColorTextureOverride / outputDepthTextureOverride} 指向本图集
 *       （这就是原版 PIP 用的公开重定向机制，<b>不需要 mixin</b>）；</li>
 *   <li>{@code projection.setupOrtho(-1000, 1000, atlasSize, atlasSize, true)} + 正交投影矩阵；</li>
 *   <li>scissor 卡住槽位；</li>
 *   <li>光照按 {@code state.usesBlockLight()} 选 {@code ITEMS_3D} / {@code ITEMS_FLAT}；</li>
 *   <li>{@code state.submit(pose, storage, light, NO_OVERLAY, 0)} +
 *       {@code featureRenderDispatcher().renderAllFeatures(storage)}；</li>
 *   <li>还原 override / scissor / pose。</li>
 * </ol>
 *
 * <h3>什么时候能烘焙</h3>
 * 这是<b>立即</b>绘制（不是排队到渲染阶段），所以<b>不能在渲染 pass 里调用</b> ——
 * 走 {@link #flush()}，由客户端 tick 末尾驱动（那时没有活动的 pass）。
 *
 * <h3>槽位管理</h3>
 * LRU：图集满了就淘汰最久没被问过的物品，腾出槽位。每次 {@link #flush()} 只烘少量
 * （{@link #BATCH_PER_FLUSH}），避免第一帧卡顿 —— 图标会分几帧陆续出现，肉眼无感。
 */
public final class McooItemIconAtlas {

    /** 图集边长（正方形）。1024 够放 21×21 个槽位，显存 4MB + 深度 4MB。 */
    private static final int ATLAS_SIZE = 1024;
    /** 单个槽位边长：界面上按 26 物理像素画，48 留了余量（GUI 缩放 2 时也不糊）。 */
    private static final int SLOT_SIZE = 48;
    private static final int COLUMNS = ATLAS_SIZE / SLOT_SIZE;
    private static final int CAPACITY = COLUMNS * COLUMNS;
    /** 每次 flush 最多烘几个：一个物品一次 draw call 级开销，别一帧烘 240 个。 */
    private static final int BATCH_PER_FLUSH = 24;

    private static final org.slf4j.Logger LOGGER =
            org.slf4j.LoggerFactory.getLogger("mcoo_build_with_ease/icons");

    /** 已烘焙的槽位数（诊断用）。 */
    private static volatile int bakedCount;

    /** 诊断开关：槽位只清成洋红、不画物品 —— 用来区分「没写进图集」和「ImGui 采不到」。 */
    public static boolean DEBUG_SOLID;

    /** 最近一次烘焙失败原因（界面上想显示就取它）。 */
    private static volatile String lastError;

    private static McooItemIconAtlas instance;

    private final GpuTexture texture;
    private final GpuTextureView textureView;
    private final GpuTexture depthTexture;
    private final GpuTextureView depthTextureView;
    private final PoseStack poseStack = new PoseStack();
    private final Projection projection = new Projection();
    private final ProjectionMatrixBuffer projectionMatrixBuffer =
            new ProjectionMatrixBuffer("mcoo item icon atlas");
    private final SubmitNodeStorage submitNodeStorage = new SubmitNodeStorage();
    private final Vector4f clearColor = new Vector4f(0f, 0f, 0f, 0f);

    /** itemId → 槽位下标；accessOrder 用于 LRU 淘汰。 */
    private final LinkedHashMap<String, Integer> slots = new LinkedHashMap<>(256, 0.75f, true);
    /** 已分配但还没烘焙的槽位（itemId → slot）。 */
    private final Map<String, Integer> pending = new LinkedHashMap<>();
    private final Deque<String> pendingOrder = new ArrayDeque<>();

    private McooItemIconAtlas() {
        var device = RenderSystem.getDevice();
        // 13 = COPY_DST | TEXTURE_BINDING | RENDER_ATTACHMENT；9 = COPY_DST | RENDER_ATTACHMENT
        this.texture = device.createTexture("mcoo item icons", 13, GpuFormat.RGBA8_UNORM,
                ATLAS_SIZE, ATLAS_SIZE, 1, 1);
        this.textureView = device.createTextureView(texture);
        this.depthTexture = device.createTexture("mcoo item icons depth", 9, GpuFormat.D32_FLOAT,
                ATLAS_SIZE, ATLAS_SIZE, 1, 1);
        this.depthTextureView = device.createTextureView(depthTexture);
        // 整张图集先清成透明：之后每个槽位自己清自己那块，避免没烘过的槽位是脏数据
        device.createCommandEncoder().clearColorAndDepthTextures(
                texture, clearColor, depthTexture, 0d);
    }

    /**
     * 确保图集已创建（<b>只能在渲染 pass 之外调用</b>，用客户端 tick）。
     *
     * <p>⚠️ 创建过程要 clearColorAndDepthTextures，在 pass 里做会让 Vulkan 掉设备 ——
     * 所以创建权收在 tick 这一处；ImGui 那边只读 {@link #get()}，没建好就先不画图标。
     */
    public static McooItemIconAtlas ensure() {
        if (instance != null) {
            return instance;
        }
        try {
            instance = new McooItemIconAtlas();
            lastError = null;
        } catch (Throwable error) {
            lastError = "create: " + error.getClass().getSimpleName() + ": " + error.getMessage();
            LOGGER.warn("[mcoo] item icon atlas create failed", error);
        }
        return instance;
    }

    /** 已创建的实例（可能为 null = 图集还没建好）。<b>不创建</b>。 */
    public static McooItemIconAtlas get() {
        return instance;
    }

    /** 图集纹理在 ImGui 里的 id（<b>每帧现取</b>，宿主会回收跨帧 id）。 */
    public long textureId() {
        return TextureBindings.INSTANCE.textureId(textureView);
    }

    /**
     * 取物品图标的 UV，并按需登记烘焙。
     *
     * @return {@code {u0,v0,u1,v1}}；槽位分配失败返回 {@code null}
     */
    public float[] uvFor(String itemId) {
        var slot = slots.get(itemId);
        if (slot == null) {
            slot = allocate(itemId);
            if (slot == null) {
                return null;
            }
        }
        int column = slot % COLUMNS;
        int row = slot / COLUMNS;
        float u0 = (float) column * SLOT_SIZE / ATLAS_SIZE;
        // V 轴必须翻转：烘焙时 pose 用的是 GUI 坐标（原点在左上、y 向下），
        // 而纹理的 v=0 在**底部**。原版 GuiItemAtlas#getOrUpdate 同样是
        // `v0 = 1 - slotY * slotUvSize`、`v1 = v0 - slotUvSize`（v0 > v1），
        // 少这一下就会去采图集的另一半 → 图标全透明（踩过）。
        float v0 = 1f - (float) row * SLOT_SIZE / ATLAS_SIZE;
        float v1 = v0 - (float) SLOT_SIZE / ATLAS_SIZE;
        return new float[]{u0, v0, u0 + (float) SLOT_SIZE / ATLAS_SIZE, v1};
    }

    private Integer allocate(String itemId) {
        int slot = slots.size();
        if (slot >= CAPACITY) {
            // LRU 淘汰最久没访问的那个
            var eldest = slots.entrySet().iterator();
            if (!eldest.hasNext()) {
                return null;
            }
            var entry = eldest.next();
            slot = entry.getValue();
            slots.remove(entry.getKey());
            pending.remove(entry.getKey());
            pendingOrder.remove(entry.getKey());
        }
        slots.put(itemId, slot);
        pending.put(itemId, slot);
        pendingOrder.addLast(itemId);
        return slot;
    }

    /**
     * 烘焙排队的图标。**只能在渲染 pass 之外调用**（用客户端 tick 末尾）。
     *
     * @return 本次烘了几个
     */
    public int flush() {
        if (pendingOrder.isEmpty()) {
            return 0;
        }
        var minecraft = Minecraft.getInstance();
        var dispatcher = minecraft.gameRenderer.featureRenderDispatcher();
        int baked = 0;
        while (baked < BATCH_PER_FLUSH && !pendingOrder.isEmpty()) {
            var itemId = pendingOrder.pollFirst();
            var slot = pending.remove(itemId);
            if (slot == null) {
                continue;
            }
            try {
                bake(minecraft, dispatcher, itemId, slot);
                baked++;
                bakedCount++;
            } catch (Throwable error) {
                // 单个物品烘焙失败（模型缺失等）不该影响其他图标
                lastError = itemId + ": " + error.getClass().getSimpleName();
                LOGGER.debug("[mcoo] item icon bake failed: {}", itemId, error);
            }
        }
        return baked;
    }

    private void bake(Minecraft minecraft, net.minecraft.client.renderer.feature.FeatureRenderDispatcher dispatcher,
                      String itemId, int slot) {
        var id = Identifier.tryParse(itemId);
        if (id == null) {
            return;
        }
        var item = BuiltInRegistries.ITEM.getValue(id);
        if (item == null) {
            return;
        }
        var state = new ItemStackRenderState();
        minecraft.getItemModelResolver().updateForTopItem(
                state,
                new net.minecraft.world.item.ItemStack(item),
                ItemDisplayContext.GUI,
                minecraft.level,
                null,
                0);

        int column = slot % COLUMNS;
        int row = slot / COLUMNS;
        int px = column * SLOT_SIZE;
        int py = row * SLOT_SIZE;
        int bottom = py + SLOT_SIZE;

        // 槽位清成透明（图标自带 alpha，背景必须干净）
        var slotClear = DEBUG_SOLID ? new Vector4f(1f, 0f, 1f, 1f) : clearColor;
        RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(
                texture, slotClear, depthTexture, 0d,
                px, ATLAS_SIZE - bottom, SLOT_SIZE, SLOT_SIZE);

        if (DEBUG_SOLID) {
            // 只验证「清屏能不能落到图集上」：跳过物品提交
            return;
        }

        poseStack.pushPose();
        poseStack.translate(px + SLOT_SIZE / 2f, py + SLOT_SIZE / 2f, 0f);
        poseStack.scale(SLOT_SIZE, -SLOT_SIZE, SLOT_SIZE);

        // 模型视图栈必须是单位阵：ItemStackRenderState#submit 会把当前栈再乘一次。
        // 原版 GuiItemAtlas 在 GuiRenderer.prepare 里调用（那时栈本来就是 GUI 上下文），
        // 我们挂在 HUD 提取阶段，栈里可能残留上一帧的世界/玩家手部矩阵 → 图标被画到槽位外、
        // 被 scissor 裁掉，表现为「烘了 240 个但全是透明」。Axiom 用的是同一个做法。
        org.joml.Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushMatrix();
        modelViewStack.identity();

        RenderSystem.outputColorTextureOverride = textureView;
        RenderSystem.outputDepthTextureOverride = depthTextureView;
        projection.setupOrtho(-1000f, 1000f, ATLAS_SIZE, ATLAS_SIZE, true);
        RenderSystem.setProjectionMatrix(
                projectionMatrixBuffer.getBuffer(projection), ProjectionType.ORTHOGRAPHIC);
        RenderSystem.enableScissorForRenderTypeDraws(px, ATLAS_SIZE - bottom, SLOT_SIZE, SLOT_SIZE);

        minecraft.gameRenderer.lighting().setupFor(
                state.usesBlockLight() ? Lighting.Entry.ITEMS_3D : Lighting.Entry.ITEMS_FLAT);

        int light = 0x00F000F0; // 与原版 GUI 物品一致的全亮光照
        state.submit(poseStack, submitNodeStorage, light, OverlayTexture.NO_OVERLAY, 0);
        dispatcher.renderAllFeatures(submitNodeStorage);

        RenderSystem.disableScissorForRenderTypeDraws();
        RenderSystem.outputColorTextureOverride = null;
        RenderSystem.outputDepthTextureOverride = null;
        modelViewStack.popMatrix();
        poseStack.popPose();
    }

    /** 已烘焙槽位数（诊断用）。 */
    public static int bakedCount() {
        return bakedCount;
    }

    /** 最近一次烘焙失败原因（null = 没失败过）。 */
    public static String lastError() {
        return lastError;
    }

    /** 调用方（HUD 钩子）捕获到异常时记一笔，别让整个 HUD 挂掉。 */
    public static void reportFailure(Throwable error) {
        lastError = "flush: " + error.getClass().getSimpleName()
                + (error.getMessage() == null ? "" : ": " + error.getMessage());
        LOGGER.warn("[mcoo] item icon flush failed", error);
    }

    /** 资源重载后图集内容作废（模型/贴图都换了）。 */
    public static void invalidate() {
        var atlas = instance;
        if (atlas != null) {
            atlas.slots.clear();
            atlas.pending.clear();
            atlas.pendingOrder.clear();
        }
    }

}
