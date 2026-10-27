package dev.huskuraft.universal.vanilla.renderer.preview;

import com.mojang.blaze3d.textures.GpuTextureView;

/**
 * 「当前这一帧的 3D 预览纹理」的发布点。
 *
 * <h3>为什么要这个</h3>
 * 结构预览走的是 MC 原生的<b>画中画（PiP）</b>：{@code GuiRenderer} 自己建离屏纹理 + 深度缓冲，
 * 调 {@code PictureInPictureRenderer#renderToTexture} 用 3D 管线画进去，然后把纹理**贴回 GUI 矩形**。
 * 那条贴图的路径对 ImGui 界面没用 —— ImGui 的面板有不透明底色，会把贴上去的图盖住。
 *
 * <p>所以 ImGui 那条路要自己采样纹理：在 {@link PreviewPictureInPictureRenderer#renderToTexture}
 * 里把基类那张纹理的 {@link GpuTextureView} 发布出来，
 * 再由业务层交给 {@code TextureBindings} 变成 ImGui 的纹理 id。
 *
 * <h3>生命周期</h3>
 * 纹理由 PiP 渲染器持有，尺寸变化/关闭时会被换掉或释放 —— 所以这里**只发布当帧的引用**，
 * 调用方每帧现取（和 {@code McooImGuiTextures} 的约定一致，不要跨帧缓存 id）。
 */
public final class PreviewTexture {

    private static volatile GpuTextureView view;
    private static volatile int width;
    private static volatile int height;

    private PreviewTexture() {
    }

    static void publish(GpuTextureView textureView, int textureWidth, int textureHeight) {
        view = textureView;
        width = textureWidth;
        height = textureHeight;
    }

    /** 当帧可用于采样的纹理；{@code null} = 这一帧没有渲染过预览。 */
    public static GpuTextureView view() {
        return view;
    }

    public static int width() {
        return width;
    }

    public static int height() {
        return height;
    }

    /** 预览不再需要时清掉，避免 ImGui 去采样一张已经失效的纹理。 */
    public static void clear() {
        view = null;
        width = 0;
        height = 0;
    }

}
