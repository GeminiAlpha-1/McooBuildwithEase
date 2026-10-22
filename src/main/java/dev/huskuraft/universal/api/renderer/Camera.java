package dev.huskuraft.universal.api.renderer;

import dev.huskuraft.universal.api.math.Quaternionf;
import dev.huskuraft.universal.api.math.Vector3d;

public interface Camera {

    Vector3d position();

    Quaternionf rotation();

    float eyeHeight();

}
