package dev.huskuraft.universal.api.events.render;


import java.io.IOException;
import java.util.function.Consumer;

import dev.huskuraft.universal.api.core.ResourceLocation;
import dev.huskuraft.universal.api.renderer.Shader;
import dev.huskuraft.universal.api.renderer.VertexFormat;

public interface RegisterShader {

    void onRegisterShader(ShadersSink sink);

    @FunctionalInterface
    interface ShadersSink {
        void register(ResourceLocation location, VertexFormat format, Consumer<Shader> consumer) throws IOException;
    }

}
