package dev.huskuraft.universal.vanilla.renderer;

import dev.huskuraft.universal.api.renderer.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

/**
 * common-api 的 {@link RenderLayer} 在 26.2 的落地形态。
 * <p>
 * 26.2 的 {@link RenderType} 构造器是 private，静态工厂 {@code RenderType.create(String, RenderSetup)}
 * 是包内可见，外部无法自建 —— 只能复用 {@code RenderTypes} 里的现成条目。
 * 所以这里额外记住「这个 layer 对应哪个纹理」，GUI 侧 {@code blit} 时要用。
 */
public record MinecraftRenderLayer(
        String name,
        RenderType renderType,
        Identifier texture
) implements RenderLayer {

    @Override
    public Object refs() {
        return renderType;
    }

}
