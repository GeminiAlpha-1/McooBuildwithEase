package dev.huskuraft.universal.api.gui;

import dev.huskuraft.universal.api.renderer.Renderer;

public interface Renderable {

    void render(Renderer renderer, int mouseX, int mouseY, float deltaTick);

    void renderOverlay(Renderer renderer, int mouseX, int mouseY, float deltaTick);

}
