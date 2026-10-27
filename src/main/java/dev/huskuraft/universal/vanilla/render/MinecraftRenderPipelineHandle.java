package dev.huskuraft.universal.vanilla.render;

import dev.huskuraft.universal.api.render.RenderPipelineHandle;
import net.minecraft.client.renderer.rendertype.RenderType;

/** 26.2 的渲染管线句柄。 */
public record MinecraftRenderPipelineHandle(
        String id,
        RenderType renderType
) implements RenderPipelineHandle {

    @Override
    public Object refs() {
        return renderType;
    }

}
