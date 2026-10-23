package dev.huskuraft.universal.vanilla.renderer.preview;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;

import dev.huskuraft.universal.api.renderer.PreviewBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 三维结构预览的离屏渲染器。
 * <p>
 * 走 MC 原版的画中画通道：{@code GuiRenderer} 会为它准备一张离屏纹理和深度缓冲，
 * 调用 {@link #renderToTexture} 把内容用<b>3D 管线</b>画进去（和世界里渲染方块同一条路），
 * 再把纹理贴回 GUI 的视口矩形。
 * <p>
 * <b>为什么不用软件投影</b>：方块实体（钟、箱子、陶罐、旗帜…）的形状
 * <b>不在方块状态模型里</b> —— 箱子/陶罐/旗帜的方块状态模型是空的（只有 particle 纹理），
 * 真正的形状由各自的方块实体渲染器在 3D 管线里画。
 * 只有走 3D 管线，才能把这些方块正确画出来。
 */
public class PreviewPictureInPictureRenderer extends PictureInPictureRenderer<PreviewRenderState> {

    private final Minecraft minecraft;

    /**
     * 方块模型状态缓存。
     * <p>
     * {@code BlockModelResolver#update} 每调用一次都会重新解析并烘焙模型（方块多的时候非常贵），
     * 而预览每帧都要重画。这里按方块状态缓存烘焙结果 —— 同一种方块只烘焙一次。
     * <p>
     * 缓存安全的依据：{@code update} 传给模型的随机种子是<b>常量 42</b>（不是按坐标算的），
     * 所以同一个方块状态在任何位置烘焙出来的结果都一样。
     */
    private final java.util.Map<BlockState, BlockModelRenderState> modelCache = new java.util.HashMap<>();
    /**
     * 缓存对应的模型集实例。资源重载后 {@code ModelManager} 会换上新的 {@code BlockModelSet}，
     * 拿它当「纪元」比一下就知道缓存过期了。
     */
    private Object modelCacheEpoch;

    public PreviewPictureInPictureRenderer(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    @Override
    public Class<PreviewRenderState> getRenderStateClass() {
        return PreviewRenderState.class;
    }

    @Override
    protected String getTextureLabel() {
        return "structure preview";
    }

    /**
     * 离屏纹理里的 Y 方向平移量。
     * <p>
     * 第一个参数才是「高度」（基类实现是 {@code return height;}，
     * {@code GuiEntityRenderer} 是 {@code return height / 2;}）。
     * <p>
     * 离屏纹理的 Y 轴**向下**（orthoInvertY），原点在左上角。
     * 返回一半 → 内容在视口里居中。
     */
    @Override
    protected float getTranslateY(int height, int guiScale) {
        return height / 2.0F;
    }

    @Override
    protected void renderToTexture(PreviewRenderState state, PoseStack poseStack, SubmitNodeCollector collector) {
        var blocks = state.blocks();
        var debug = Boolean.getBoolean("mcoo.debugPreview");
        if (blocks == null || blocks.isEmpty()) {
            return;
        }
        var startNanos = System.nanoTime();

        // GUI 里的 3D 内容用 ENTITY_IN_UI 这套光照（和物品、实体在 GUI 里显示时一样）
        var lighting = minecraft.gameRenderer.lighting();
        lighting.setupFor(Lighting.Entry.ENTITY_IN_UI);

        // 结构包围盒（方块单位）
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        for (var block : blocks) {
            var position = block.position();
            minX = Math.min(minX, position.x());
            maxX = Math.max(maxX, position.x());
            minY = Math.min(minY, position.y());
            maxY = Math.max(maxY, position.y());
            minZ = Math.min(minZ, position.z());
            maxZ = Math.max(maxZ, position.z());
        }
        var centerX = (minX + maxX + 1) / 2.0;
        var centerY = (minY + maxY + 1) / 2.0;
        var centerZ = (minZ + maxZ + 1) / 2.0;

        // 【离屏纹理是正交投影】视锥固定为 X∈[0,width]、Y∈[0,height]（物理像素）。
        // PictureInPictureRenderer#prepare 已经把传入的 poseStack 摆好了：
        //   translate(viewportWidth/2, getTranslateY(...), 0) → scale(guiScale × state.scale(), ..., -...)
        // 缩放的比例由 state.scale() 统一负责（见 PreviewRenderState#scale），
        // 这里**不能再自己缩放**（叠加两次就会撑出视口被裁掉），
        // 只负责在这个坐标系里摆方块：按相机旋转，再把结构中心对到原点。
        var camera = state.camera();
        var yaw = (float) Math.toRadians(camera.yaw());
        var pitch = (float) Math.toRadians(camera.pitch());
        // 缩放系数：视口里能看到的模型单位数 = 视口逻辑宽 / state.scale()
        var modelScale = Math.max(0.0001F, state.scale());

        // 相机：绕结构中心旋转（偏航绕 Y、俯仰绕 X），再把结构中心对到原点
        poseStack.pushPose();
        // 平移（拖动平移）：平移量按逻辑像素给，换算成模型单位。
        // Y 轴向上，所以 Y 的平移量取负。
        poseStack.translate(camera.panX() / modelScale, -camera.panY() / modelScale, 0.0F);
        // 【旋转顺序】这里是 Rx(俯仰) × Ry(偏航)：
        // 偏航绕的是世界的竖直轴，俯仰绕的是偏航之后的水平轴 ——
        // 所以世界的竖直方向在屏幕上永远是直的（等距视角），不会跟着偏航歪掉。
        // 注意别用 rotateYXZ：那个是 Ry × Rx，等于把相机挪到了结构的另一侧（画面会镜像）。
        poseStack.mulPose(new org.joml.Quaternionf().rotateXYZ(pitch, yaw, 0.0F));
        poseStack.translate((float) -centerX, (float) -centerY, (float) -centerZ);

        if (debug) {
            // 结构中心变换后的位置 —— 应该是视口中心
            var m = poseStack.last().pose();
            var probe = m.transformPosition(
                    new org.joml.Vector3f((float) centerX, (float) centerY, (float) centerZ));
            System.out.println("[preview] center-> " + probe
                    + " viewport=" + state.x0() + "," + state.y0() + " " + (state.x1() - state.x0()) + "x" + (state.y1() - state.y0())
                    + " scale=" + state.scale());
        }

        var resolver = new BlockModelResolver(minecraft.getModelManager());
        var dispatcher = minecraft.getBlockEntityRenderDispatcher();
        var crumbling = new ModelFeatureRenderer.CrumblingOverlay(0, poseStack.last());
        int modelCount = 0;
        int entityCount = 0;

        // 方块实体的「要不要渲染」判定是**距离相机多远**（默认 64 格）。
        // 把相机放在预览中心 —— 方块实体用的是局部坐标，都在附近，必然通过。
        dispatcher.prepare(new net.minecraft.world.phys.Vec3(0.0, 0.0, 0.0));

        // 资源重载后模型集会换新实例 → 缓存作废
        var epoch = minecraft.getModelManager().getBlockModelSet();
        if (this.modelCacheEpoch != epoch) {
            this.modelCacheEpoch = epoch;
            this.modelCache.clear();
        }

        for (var block : blocks) {
            var blockModelState = (BlockState) block.state().reference();
            if (blockModelState == null) {
                continue;
            }
            var position = block.position();
            // 方块实体用**局部坐标**（相对结构中心）创建：
            // 渲染位置由 poseStack 决定，而方块实体的坐标只影响
            // 「离相机太远要不要渲染」的距离判定 —— 局部坐标都在相机附近，必然通过。
            var localPos = new BlockPos(
                    (int) Math.round(position.x() - centerX),
                    (int) Math.round(position.y() - centerY),
                    (int) Math.round(position.z() - centerZ));

            // 位姿：方块在自己的格子里（方块模型是局部坐标 0..1）
            poseStack.pushPose();
            poseStack.translate(position.x(), position.y(), position.z());

            // 1) 方块模型（方块状态模型里的几何体）
            var modelState = modelCache.get(blockModelState);
            if (modelState == null) {
                modelState = new BlockModelRenderState();
                resolver.update(modelState, blockModelState, BlockDisplayContext.create());
                modelCache.put(blockModelState, modelState);
            }
            if (!modelState.isEmpty()) {
                // overlay 必须是原版的 NO_OVERLAY（u | v<<16）——传 -1 会采到 overlay 贴图的错误格子，
                // 方块模型会整个变黑。
                modelState.submit(poseStack, collector, 0xF000F0,
                        net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
                modelCount++;
            }

            // 2) 方块实体（钟、箱子、陶罐、旗帜…的形状只在这里）
            if (renderBlockEntity(block, localPos, blockModelState, poseStack, collector, dispatcher, crumbling)) {
                entityCount++;
            }

            poseStack.popPose();
        }
        poseStack.popPose();
        if (debug) {
            var millis = (System.nanoTime() - startNanos) / 1_000_000.0;
            System.out.println("[preview] rendered models=" + modelCount + " entities=" + entityCount
                    + " cached=" + modelCache.size() + " cost=" + String.format("%.1f", millis) + "ms");
        }
    }

    private boolean renderBlockEntity(PreviewBlock block,
                                      BlockPos localPos,
                                      BlockState blockState,
                                      PoseStack poseStack,
                                      SubmitNodeCollector collector,
                                      BlockEntityRenderDispatcher dispatcher,
                                      ModelFeatureRenderer.CrumblingOverlay crumbling) {
        var debug = Boolean.getBoolean("mcoo.debugPreview");
        BlockEntity entity = null;
        try {
            // 优先用快照里带来的方块实体 NBT 还原（钟的朝向、箱子的类型等都在里面）
            var entityTag = block.entityTag();
            if (entityTag != null) {
                var tag = (net.minecraft.nbt.CompoundTag) entityTag.refs();
                entity = BlockEntity.loadStatic(localPos, blockState, tag, null);
            }
            if (entity == null) {
                // 没有 NBT 就按方块状态新建一个（钟、箱子这类自带默认值的方块实体）
                var blockType = blockState.getBlock();
                if (blockType instanceof net.minecraft.world.level.block.EntityBlock entityBlock) {
                    entity = entityBlock.newBlockEntity(localPos, blockState);
                }
            }
            if (entity == null) {
                return false;
            }
            // 【必须挂到 level 上】BlockEntityRenderDispatcher#tryExtractRenderState 里
            // 第一件事就是查 entity.hasLevel()，没有 level 直接返回 null（什么都不渲染）。
            // level 用客户端当前世界就行 —— 方块实体只是借它做数据载体，
            // 渲染位置由 poseStack 决定，不受 level 里的实际坐标影响。
            if (!entity.hasLevel()) {
                var level = minecraft.level;
                if (level != null) {
                    entity.setLevel(level);
                }
            }
        } catch (Throwable throwable) {
            if (debug) {
                System.out.println("[preview] be create failed: " + blockState.getBlock() + " -> " + throwable);
            }
            return false;
        }
        try {
            var renderState = dispatcher.tryExtractRenderState(entity, 0.0F, crumbling, false);
            if (renderState == null) {
                // 有些方块实体的渲染器会自己判断「值不值得画」（比如信标只在自己有光柱时才画），
                // 这种情况下拿不到渲染状态是正常的 —— 方块状态模型那部分已经画上了。
                if (debug) {
                    System.out.println("[preview] be skipped: " + blockState.getBlock());
                }
                return false;
            }
            // cameraState 只用于光照方向之类的信息，离屏渲染里给一个默认值即可
            // （GuiEntityRenderer 也是自己 new 一个 CameraRenderState）。
            dispatcher.submit(renderState, poseStack, collector, new net.minecraft.client.renderer.state.level.CameraRenderState());
            return true;
        } catch (Throwable throwable) {
            // 某个方块实体渲染失败不应该让整个预览挂掉
            if (debug) {
                System.out.println("[preview] be render failed: " + blockState.getBlock() + " -> " + throwable);
            }
            return false;
        }
    }
}
