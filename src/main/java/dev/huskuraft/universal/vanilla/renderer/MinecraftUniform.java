package dev.huskuraft.universal.vanilla.renderer;

import dev.huskuraft.universal.api.renderer.Uniform;

/**
 * 26.2 删除了 {@code com.mojang.blaze3d.shaders.Uniform}。
 * <p>
 * 新的体系里 uniform 由 {@code RenderPipeline} 的 {@code BindGroupLayout} 声明，
 * 值通过 {@code net.minecraft.client.renderer.UniformValue} 写进 UBO ——
 * 那是个只写接口，外部拿不到「可寻址的 uniform 句柄」。
 * <p>
 * 所以这里保留 common-api 的 {@link Uniform} 契约，但所有 setter 都是空实现：
 * <b>依赖逐帧设置 uniform 的视觉会退化</b>（上游的自定义着色器已随本次移植一并放弃，
 * 业务层目前没有依赖 uniform 的代码路径）。
 */
public record MinecraftUniform() implements Uniform {

    @Override
    public Object refs() {
        return null;
    }

    @Override
    public void set(float x) {
    }

    @Override
    public void set(float x, float y) {
    }

    @Override
    public void set(float x, float y, float z) {
    }

    @Override
    public void set(float x, float y, float z, float w) {
    }

    @Override
    public void setSafe(float x, float y, float z, float w) {
    }

    @Override
    public void setSafe(int x, int y, int z, int w) {
    }

    @Override
    public void set(int x) {
    }

    @Override
    public void set(int x, int y) {
    }

    @Override
    public void set(int x, int y, int z) {
    }

    @Override
    public void set(int x, int y, int z, int w) {
    }

    @Override
    public void set(float[] values) {
    }

    @Override
    public void setMatrix22(float m00, float m01, float m10, float m11) {
    }

    @Override
    public void setMatrix23(float m00, float m01, float m02, float m10, float m11, float m12) {
    }

    @Override
    public void setMatrix24(float m00, float m01, float m02, float m03, float m10, float m11, float m12, float m13) {
    }

    @Override
    public void setMatrix32(float m00, float m01, float m10, float m11, float m20, float m21) {
    }

    @Override
    public void setMatrix33(float m00, float m01, float m02, float m10, float m11, float m12, float m20, float m21, float m22) {
    }

    @Override
    public void setMatrix34(float m00, float m01, float m02, float m03, float m10, float m11, float m12, float m13, float m20, float m21, float m22, float m23) {
    }

    @Override
    public void setMatrix42(float m00, float m01, float m02, float m03, float m10, float m11, float m12, float m13) {
    }

    @Override
    public void setMatrix43(float m00, float m01, float m02, float m03, float m10, float m11, float m12, float m13, float m20, float m21, float m22, float m23) {
    }

    @Override
    public void setMatrix44(float m00, float m01, float m02, float m03, float m10, float m11, float m12, float m13, float m20, float m21, float m22, float m23, float m30, float m31, float m32, float m33) {
    }

}
