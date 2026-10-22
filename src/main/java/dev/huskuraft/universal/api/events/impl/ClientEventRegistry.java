package dev.huskuraft.universal.api.events.impl;

import dev.huskuraft.universal.api.events.Event;
import dev.huskuraft.universal.api.events.input.InteractionInput;
import dev.huskuraft.universal.api.events.input.KeyInput;
import dev.huskuraft.universal.api.events.input.RegisterKeys;
import dev.huskuraft.universal.api.events.lifecycle.ClientStart;
import dev.huskuraft.universal.api.events.lifecycle.ClientTick;
import dev.huskuraft.universal.api.events.render.RegisterShader;
import dev.huskuraft.universal.api.events.render.RenderGui;
import dev.huskuraft.universal.api.events.render.RenderWorld;

public class ClientEventRegistry extends EventRegistry {

    public Event<RegisterKeys> getRegisterKeysEvent() {
        return get();
    }

    public Event<KeyInput> getKeyInputEvent() {
        return get();
    }

    public Event<InteractionInput> getInteractionInputEvent() {
        return get();
    }

    public Event<ClientStart> getClientStartEvent() {
        return get();
    }

    public Event<ClientTick> getClientTickEvent() {
        return get();
    }

    public Event<RenderGui> getRenderGuiEvent() {
        return get();
    }

    public Event<RenderWorld> getRenderWorldEvent() {
        return get();
    }

    public Event<RegisterShader> getRegisterShaderEvent() {
        return get();
    }

}
