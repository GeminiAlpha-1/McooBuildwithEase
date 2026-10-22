package dev.huskuraft.universal.api.events.render;

import dev.huskuraft.universal.api.renderer.Renderer;

@FunctionalInterface
public interface RenderGui {
    void onRenderGui(Renderer renderer, float deltaTick);
}
