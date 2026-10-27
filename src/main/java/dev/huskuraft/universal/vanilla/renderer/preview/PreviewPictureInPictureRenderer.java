package dev.huskuraft.universal.vanilla.renderer.preview;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;

/**
 * 三维结构预览的离屏渲染器。
 * <p>
 * 走 MC 原版的画中画（PiP）通道：{@code GuiRenderer} 会为它准备一张离屏纹理和深度缓冲，
 * 调用 {@link #renderToTexture} 把内容用<b>3D 管线</b>画进去（和世界里渲染方块同一条路），
 * 再把纹理贴回 GUI 的对应矩形。
 * <p>
 * <b>为什么不用软件投影</b>：方块实体（钟、箱子、陶罐、旗帜…）的形状
 * <b>不在方块状态模型里</b> —— 箱子/陶罐/旗帜的方块状态模型是空的（只有 particle 纹理），
 * 真正的形状由各自的方块实体渲染器在 3D 管线里画。
 * 只有走 3D 管线，才能把这些方块正确画出来。
 */
public class PreviewPictureInPictureRenderer extends PictureInPictureRenderer<PreviewRenderState> {

    private final Minecraft minecraft;
    private final PreviewScene scene;

    public PreviewPictureInPictureRenderer(Minecraft minecraft) {
        this.minecraft = minecraft;
        this.scene = new PreviewScene(minecraft);
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
        if (blocks == null || blocks.isEmpty()) {
            return;
        }
        if (Boolean.getBoolean("mcoo.debugPreview")) {
            var m = poseStack.last().pose();
            var probe = m.transformPosition(new org.joml.Vector3f(0.0F, 0.0F, 0.0F));
            System.out.println("[preview] center-> " + probe
                    + " viewport=" + state.x0() + "," + state.y0() + " "
                    + (state.x1() - state.x0()) + "x" + (state.y1() - state.y0())
                    + " scale=" + state.scale());
        }
        // 缩放的比例由 state.scale() 统一负责（见 PreviewRenderState#scale），
        // 这里不再自己缩放（叠加两次就会撑出视口被裁掉）。
        scene.render(blocks, state.camera(), state.orientation(),
                Math.max(0.0001F, state.scale()), poseStack, collector);

        // 把基类那张离屏纹理发布出去，供 ImGui 界面采样（见 PreviewTexture 的注释）。
        // textureView 是 PictureInPictureRenderer 的 private 字段，靠 class tweaker 打开
        // （mcoo_build_with_ease.classtweaker 里的 accessible field 条目）。
        PreviewTexture.publish(this.textureView,
                state.x1() - state.x0(), state.y1() - state.y0());
    }
}
