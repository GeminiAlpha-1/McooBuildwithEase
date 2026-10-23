package dev.huskuraft.universal.vanilla.renderer.preview;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;

import dev.huskuraft.universal.api.renderer.PreviewBlock;
import dev.huskuraft.universal.api.renderer.PreviewCamera;
import dev.huskuraft.universal.api.renderer.PreviewOrientation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Map;

/**
 * 预览场景的绘制：方块模型 + 方块实体 + 含水方块的水。
 * <p>
 * 预览和「导出图片」两条路都用这个类，保证画面完全一致。
 */
public final class PreviewScene {

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
    private final Map<BlockState, BlockModelRenderState> modelCache = new java.util.HashMap<>();
    /**
     * 缓存对应的模型集实例。资源重载后 {@code ModelManager} 会换上新的 {@code BlockModelSet}，
     * 拿它当「纪元」比一下就知道缓存过期了。
     */
    private Object modelCacheEpoch;

    public PreviewScene(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    /**
     * 画一个场景。
     *
     * @param blocks      要画的方块
     * @param camera      相机
     * @param orientation 朝向修正
     * @param modelScale  缩放系数（视口里能看到的模型单位数 = 视口逻辑宽 / modelScale）
     * @param poseStack   位姿（画中画通道已经摆好了投影和平移，这里只管摆方块）
     * @param collector   提交器
     */
    public void render(List<PreviewBlock> blocks, PreviewCamera camera, PreviewOrientation orientation,
                       float modelScale, PoseStack poseStack, SubmitNodeCollector collector) {
        if (blocks == null || blocks.isEmpty()) {
            return;
        }
        var debug = Boolean.getBoolean("mcoo.debugPreview");
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

        // 相机：绕结构中心旋转（偏航绕 Y、俯仰绕 X），再把结构中心对到原点
        poseStack.pushPose();
        // 平移（拖动平移）：平移量按逻辑像素给，换算成模型单位。
        // 【符号】平移是在**旋转之后**加到视图坐标系里的（矩阵链是 T(平移)·R·T(居中)），
        // 而视图的 +Y 在最终图像里是向下的 ——
        // 所以往下拖（panY 变大）就该给视图的 +Y，符号取正。
        poseStack.translate(camera.panX() / modelScale, camera.panY() / modelScale, 0.0F);
        // 旋转：绕世界轴的 90° 步进 × (Rx(180°+俯仰) × Ry(偏航))。
        // 细节见 {@link PreviewRotation}（缩放那边也用同一份矩阵）。
        poseStack.mulPose(PreviewRotation.of(camera, orientation));
        poseStack.translate((float) -centerX, (float) -centerY, (float) -centerZ);

        var resolver = new BlockModelResolver(minecraft.getModelManager());
        var dispatcher = minecraft.getBlockEntityRenderDispatcher();
        // 破坏进度覆盖层必须是 null —— 原版只在方块真的在被破坏时才传它。
        // 传一个进度为 0 的覆盖层等于告诉渲染器「这个方块正在碎裂」，
        // 结果钟、箱子、旗帜这些方块实体上会浮出方块破坏时的裂纹。
        ModelFeatureRenderer.CrumblingOverlay crumbling = null;
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

        // 结构里带流体的方块（用来判断「两块水之间的内部面」要不要画）
        Map<BlockPos, net.minecraft.world.level.material.FluidState> fluids = new java.util.HashMap<>();
        for (var block : blocks) {
            var state = (BlockState) block.state().reference();
            if (state == null) {
                continue;
            }
            var fluidState = state.getFluidState();
            if (!fluidState.isEmpty()) {
                var position = block.position();
                fluids.put(new BlockPos(position.x(), position.y(), position.z()), fluidState);
            }
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
                modelState.submit(poseStack, collector, lightCoords(blockModelState),
                        net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
                modelCount++;
            }

            // 1.5) 含水的方块：水不是方块模型的一部分（原版由流体渲染器单独画），
            //     这里按原版 FluidRenderer 的规则补画（见 renderFluid）。
            renderFluid(blockModelState, new BlockPos(position.x(), position.y(), position.z()),
                    fluids, poseStack, collector);

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

    /**
     * 环境光等级（当成白天的天光）。
     * <p>
     * 预览里没有世界，方块拿不到真实的光照 —— 之前一律用 0xF000F0（天光、块光全满），
     * 结果所有方块亮度一样，画面很平。
     */
    private static final int AMBIENT_LIGHT = 15;

    /**
     * 按方块自己的发光等级算光照坐标。
     * <p>
     * 原版里萤石、海晶灯这类方块「自带光」，靠的是它们所在位置的光照等级（lightmap），
     * 并没有单独的渲染层。预览里没有世界可查，就按方块自己的发光等级来给：
     * 普通方块只有环境光，发光方块额外带上它自己的发光等级 —— 这样它们会明显更亮、更暖。
     *
     * @param state 方块状态
     * @return 光照坐标（blockLight 高位、skyLight 低位）
     */
    private static int lightCoords(BlockState state) {
        var emission = state.getLightEmission();
        if (emission <= 0) {
            return AMBIENT_LIGHT << 4;
        }
        return (emission << 20) | (AMBIENT_LIGHT << 4);
    }

    /**
     * 含水的方块：把水也画出来。
     * <p>
     * 原版里水**不是**方块模型的一部分，而是由流体渲染器单独画的，
     * 而且**画哪些面是有讲究的**（{@code FluidRenderer#shouldRenderFace}）：
     * <ul>
     *     <li>方块自己的形状挡住的面不画 —— 所以实心方块（含水树叶）里其实<b>看不到水</b>，
     *         这和游戏里一致；</li>
     *     <li>邻居是同一种流体的面也不画（避免两块水之间出现内部面）。</li>
     * </ul>
     * 之前不管不顾地画一个完整的水立方体，结果含水树叶里透出来的全是水立方体的<b>内壁</b> ——
     * 看着像渲染坏了。这里按原版的规则来。
     */
    private void renderFluid(BlockState blockState, net.minecraft.core.BlockPos position,
                             Map<net.minecraft.core.BlockPos, net.minecraft.world.level.material.FluidState> fluids,
                             PoseStack poseStack, SubmitNodeCollector collector) {
        try {
            var fluidState = blockState.getFluidState();
            if (fluidState.isEmpty()) {
                return;
            }
            var fluidModel = minecraft.getModelManager().getFluidStateModelSet().get(fluidState);
            if (fluidModel == null) {
                return;
            }
            var material = fluidModel.stillMaterial();
            var sprite = material.sprite();
            var materialInfo = net.minecraft.client.resources.model.geometry.BakedQuad.MaterialInfo.of(
                    material, com.mojang.blaze3d.platform.Transparency.TRANSLUCENT, 0, true, 0);

            // 流体高度：块里的源流体是满格的（游戏里含水的方块里水就是灌满的）
            var height = fluidState.getOwnHeight();
            if (height <= 0.0F) {
                return;
            }

            // 要画哪些面（原版 FluidRenderer 的规则）
            var quads = new java.util.ArrayList<net.minecraft.client.resources.model.geometry.BakedQuad>(6);
            for (var direction : net.minecraft.core.Direction.values()) {
                if (isFaceOccludedBySelf(blockState, direction)) {
                    continue;
                }
                var neighbor = fluids.get(position.relative(direction));
                if (neighbor != null && neighbor.getType().isSame(fluidState.getType())) {
                    // 邻居也是同一种流体，这一面是两块水之间的内部面，不画
                    continue;
                }
                quads.add(fluidQuad(direction, height, sprite, materialInfo));
            }
            if (quads.isEmpty()) {
                return;
            }
            var renderType = net.minecraft.client.renderer.rendertype.RenderTypes.translucentMovingBlock();
            collector.submitCustomGeometry(poseStack, renderType, (pose, consumer) -> {
                var instance = new com.mojang.blaze3d.vertex.QuadInstance();
                instance.setLightCoords(lightCoords(blockState));
                instance.setOverlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
                // 水的颜色（原版取的是 biome 的水色，主世界就是这个蓝）；岩浆用自己的贴图颜色，不染色
                var tint = fluidState.getType() == net.minecraft.world.level.material.Fluids.WATER
                        ? 0xFF3F76E4 : 0xFFFFFFFF;
                instance.setColor(tint);
                for (var quad : quads) {
                    consumer.putBakedQuad(pose, quad, instance);
                }
            });
        } catch (Throwable throwable) {
            // 流体画不出来也不能影响方块本身
            if (Boolean.getBoolean("mcoo.debugPreview")) {
                System.out.println("[preview] fluid render failed: " + throwable);
            }
        }
    }

    /**
     * 方块自己的形状挡住了这个面吗（原版 {@code FluidRenderer#isFaceOccludedBySelf}）。
     */
    private static boolean isFaceOccludedBySelf(BlockState state, net.minecraft.core.Direction direction) {
        var shape = state.getFaceOcclusionShape(direction);
        if (shape == net.minecraft.world.phys.shapes.Shapes.empty()) {
            return false;
        }
        if (shape == net.minecraft.world.phys.shapes.Shapes.block()) {
            // 整个格子都是方块 → 这一面被挡住（高度恒为 1，所以第二个条件永远成立）
            return true;
        }
        var box = net.minecraft.world.phys.shapes.Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
        return net.minecraft.world.phys.shapes.Shapes.blockOccludes(box, shape, direction.getOpposite());
    }

    /**
     * 流体的一个面（局部坐标 0~1）。
     * <p>
     * 顶点顺序和原版方块模型一致（从面外侧看是逆时针），UV 统一用整张贴图。
     * <p>
     * 【注意】X / Z 必须覆盖**整个格子**（0~1）。之前这里写的是 0~0.5 ——
     * 水方块只有半格宽，看起来是个长条，而且能直接看进水立方体的内壁。
     *
     * @param direction 面朝向
     * @param height    流体高度（顶面在这个高度）
     */
    private static net.minecraft.client.resources.model.geometry.BakedQuad fluidQuad(
            net.minecraft.core.Direction direction, float height,
            net.minecraft.client.renderer.texture.TextureAtlasSprite sprite,
            net.minecraft.client.resources.model.geometry.BakedQuad.MaterialInfo materialInfo) {
        var u0 = sprite.getU0();
        var v0 = sprite.getV0();
        var u1 = sprite.getU1();
        var v1 = sprite.getV1();
        var uvTopLeft = net.minecraft.client.model.geom.builders.UVPair.pack(u0, v0);
        var uvTopRight = net.minecraft.client.model.geom.builders.UVPair.pack(u1, v0);
        var uvBottomRight = net.minecraft.client.model.geom.builders.UVPair.pack(u1, v1);
        var uvBottomLeft = net.minecraft.client.model.geom.builders.UVPair.pack(u0, v1);
        float h = height;
        return switch (direction) {
            // 顶面（+Y）
            case UP -> quad(1.0F, h, 0.0F, 1.0F, h, 1.0F, 0.0F, h, 1.0F, 0.0F, h, 0.0F,
                    direction, uvTopLeft, uvTopRight, uvBottomRight, uvBottomLeft, materialInfo);
            // 底面（-Y）
            case DOWN -> quad(1.0F, 0.0F, 1.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F,
                    direction, uvTopLeft, uvTopRight, uvBottomRight, uvBottomLeft, materialInfo);
            // -X 面
            case WEST -> quad(0.0F, h, 0.0F, 0.0F, h, 1.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F,
                    direction, uvTopLeft, uvTopRight, uvBottomRight, uvBottomLeft, materialInfo);
            // +X 面
            case EAST -> quad(1.0F, h, 1.0F, 1.0F, h, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F, 0.0F, 1.0F,
                    direction, uvTopLeft, uvTopRight, uvBottomRight, uvBottomLeft, materialInfo);
            // +Z 面
            case SOUTH -> quad(0.0F, h, 1.0F, 1.0F, h, 1.0F, 1.0F, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F,
                    direction, uvTopLeft, uvTopRight, uvBottomRight, uvBottomLeft, materialInfo);
            // -Z 面
            case NORTH -> quad(1.0F, h, 0.0F, 0.0F, h, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F,
                    direction, uvTopLeft, uvTopRight, uvBottomRight, uvBottomLeft, materialInfo);
        };
    }


    private static net.minecraft.client.resources.model.geometry.BakedQuad quad(
            float x0, float y0, float z0,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            net.minecraft.core.Direction direction,
            long uv0, long uv1, long uv2, long uv3,
            net.minecraft.client.resources.model.geometry.BakedQuad.MaterialInfo materialInfo) {
        return new net.minecraft.client.resources.model.geometry.BakedQuad(
                new org.joml.Vector3f(x0, y0, z0),
                new org.joml.Vector3f(x1, y1, z1),
                new org.joml.Vector3f(x2, y2, z2),
                new org.joml.Vector3f(x3, y3, z3),
                uv0, uv1, uv2, uv3, direction, materialInfo);
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
