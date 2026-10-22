package dev.huskuraft.universal.api.events.render;

import dev.huskuraft.universal.api.renderer.Renderer;

@FunctionalInterface
public interface RenderWorld {
    void onRenderWorld(Renderer renderer, float deltaTick);
}
