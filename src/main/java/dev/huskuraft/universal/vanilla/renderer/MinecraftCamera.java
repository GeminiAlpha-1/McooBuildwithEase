package dev.huskuraft.universal.vanilla.renderer;

import dev.huskuraft.universal.api.math.Quaternionf;
import dev.huskuraft.universal.api.math.Vector3d;
import dev.huskuraft.universal.api.renderer.Camera;
import dev.huskuraft.universal.vanilla.core.MinecraftConvertor;

/**
 * 26.2 的 {@code Camera} 把 {@code getPosition()} / {@code getEntity()} 改成了
 * {@code position()} / {@code entity()}。
 */
public record MinecraftCamera(
        net.minecraft.client.Camera refs
) implements Camera {

    @Override
    public Vector3d position() {
        return MinecraftConvertor.fromPlatformVector3d(refs.position());
    }

    @Override
    public Quaternionf rotation() {
        return MinecraftConvertor.fromPlatformQuaternion(refs.rotation());
    }

    @Override
    public float eyeHeight() {
        var entity = refs.entity();
        return entity == null ? 0.0F : entity.getEyeHeight();
    }

}
