package dev.huskuraft.universal.api.renderer;

import dev.huskuraft.universal.api.math.Matrix3f;
import dev.huskuraft.universal.api.math.Matrix4f;
import dev.huskuraft.universal.api.math.Quaternionf;
import dev.huskuraft.universal.api.platform.PlatformReference;

public interface MatrixStack extends PlatformReference {

    void push();

    void pop();

    Matrix last();

    void translate(float x, float y, float z);

    void scale(float x, float y, float z);

    void rotate(Quaternionf quaternion);

    void multiply(Matrix4f matrix);

    void identity();

    interface Matrix {

        Matrix4f pose();

        Matrix3f normal();

    }

}
