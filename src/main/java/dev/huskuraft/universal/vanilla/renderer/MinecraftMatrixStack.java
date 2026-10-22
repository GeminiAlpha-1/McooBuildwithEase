package dev.huskuraft.universal.vanilla.renderer;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.huskuraft.universal.api.math.Matrix3f;
import dev.huskuraft.universal.api.math.Matrix4f;
import dev.huskuraft.universal.api.math.Quaternionf;
import dev.huskuraft.universal.api.renderer.MatrixStack;
import dev.huskuraft.universal.vanilla.core.MinecraftConvertor;
import org.joml.Matrix3x2fStack;

/**
 * 26.2 的矩阵栈有两种形态：
 * <ul>
 *     <li>世界渲染 → 3D 的 {@link PoseStack}（没变）；</li>
 *     <li>GUI 渲染 → 2D 的 {@link Matrix3x2fStack}（{@code GuiGraphicsExtractor#pose()}）。</li>
 * </ul>
 * 2D 没有 z 轴，这里把它展开成 4x4 交给 common-api（z 恒为 0），
 * 法线矩阵取 4x4 的左上 3x3。
 */
public class MinecraftMatrixStack implements MatrixStack {

    /** 2D 矩阵栈的栈深度。 */
    private static final int STACK_DEPTH = 64;

    private final PoseStack poseStack;
    private final Matrix3x2fStack matrix2d;

    public MinecraftMatrixStack(PoseStack poseStack) {
        this.poseStack = poseStack;
        this.matrix2d = null;
    }

    public MinecraftMatrixStack(Matrix3x2fStack matrix2d) {
        this.poseStack = null;
        this.matrix2d = matrix2d;
    }

    @Override
    public Object refs() {
        return poseStack != null ? poseStack : matrix2d;
    }

    @Override
    public void push() {
        if (poseStack != null) {
            poseStack.pushPose();
        } else {
            matrix2d.pushMatrix();
        }
    }

    @Override
    public void pop() {
        if (poseStack != null) {
            poseStack.popPose();
        } else {
            matrix2d.popMatrix();
        }
    }

    @Override
    public Matrix last() {
        return new Matrix() {
            @Override
            public Matrix4f pose() {
                if (poseStack != null) {
                    return MinecraftConvertor.fromPlatformMatrix4f(poseStack.last().pose());
                }
                return MinecraftConvertor.fromPlatformMatrix4f(toMatrix4f(matrix2d));
            }

            @Override
            public Matrix3f normal() {
                if (poseStack != null) {
                    return MinecraftConvertor.fromPlatformMatrix3f(poseStack.last().normal());
                }
                return MinecraftConvertor.fromPlatformMatrix3f(toMatrix3f(matrix2d));
            }
        };
    }

    @Override
    public void translate(float x, float y, float z) {
        if (poseStack != null) {
            poseStack.translate(x, y, z);
        } else {
            matrix2d.translate(x, y);
        }
    }

    @Override
    public void scale(float x, float y, float z) {
        if (poseStack != null) {
            poseStack.scale(x, y, z);
        } else {
            matrix2d.scale(x, y);
        }
    }

    @Override
    public void rotate(Quaternionf quaternion) {
        if (poseStack != null) {
            poseStack.mulPose(MinecraftConvertor.toPlatformQuaternion(quaternion));
        }
        // 2D 矩阵表达不了 3D 旋转，忽略
    }

    @Override
    public void multiply(Matrix4f matrix) {
        if (poseStack != null) {
            poseStack.mulPose(MinecraftConvertor.toPlatformMatrix4f(matrix));
        }
        // 2D 无法表达 4x4 乘法，忽略
    }

    @Override
    public void identity() {
        if (poseStack != null) {
            poseStack.setIdentity();
        } else {
            matrix2d.identity();
        }
    }

    /**
     * 2D 栈 → 4x4。
     * <p>
     * JOML 的 {@code Matrix3x2f} 是列主序，列分别是
     * {@code (m00,m01)}、{@code (m10,m11)}、{@code (m20,m21)=平移}。
     */
    private static org.joml.Matrix4f toMatrix4f(Matrix3x2fStack matrix) {
        return new org.joml.Matrix4f(
                matrix.m00(), matrix.m01(), 0.0F, 0.0F,
                matrix.m10(), matrix.m11(), 0.0F, 0.0F,
                0.0F, 0.0F, 1.0F, 0.0F,
                matrix.m20(), matrix.m21(), 0.0F, 1.0F
        );
    }

    /** 2D 栈 → 3x3 法线矩阵。2D 没有法线概念，返回单位矩阵。 */
    private static org.joml.Matrix3f toMatrix3f(Matrix3x2fStack matrix) {
        return new org.joml.Matrix3f();
    }

    /** 创建一个空的 2D 矩阵栈（GUI 模式用）。 */
    public static Matrix3x2fStack createMatrix2d() {
        return new Matrix3x2fStack(STACK_DEPTH);
    }

}
