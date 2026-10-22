package dev.huskuraft.universal.api.renderer;

import dev.huskuraft.universal.api.platform.PlatformReference;

public interface BufferSource extends PlatformReference {

    VertexBuffer getBuffer(RenderLayer renderLayer);

    void end();

}
