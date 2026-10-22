package dev.huskuraft.universal.fabric.events;

import com.google.auto.service.AutoService;
import dev.huskuraft.universal.api.core.InteractionType;
import dev.huskuraft.universal.api.events.impl.ClientEventRegistry;
import dev.huskuraft.universal.api.events.lifecycle.ClientTick;
import dev.huskuraft.universal.api.input.InputKey;
import dev.huskuraft.universal.fabric.events.common.InteractionInputEvents;
import dev.huskuraft.universal.fabric.events.common.PlatformLifecycleEvents;
import dev.huskuraft.universal.fabric.networking.FabricNetworking;
import dev.huskuraft.universal.vanilla.core.MinecraftConvertor;
import dev.huskuraft.universal.vanilla.platform.MinecraftClient;
import dev.huskuraft.universal.vanilla.renderer.MinecraftRenderer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

@AutoService(ClientEventRegistry.class)
public class FabricClientEventRegistry extends ClientEventRegistry {

    public FabricClientEventRegistry() {
        PlatformLifecycleEvents.CLIENT_START.register(() -> {
            getClientStartEvent().invoker().onClientStart(new MinecraftClient(Minecraft.getInstance()));
            getRegisterNetworkEvent().invoker().onRegisterNetwork(FabricNetworking::register);

            getRegisterKeysEvent().invoker().onRegisterKeys(key -> {
                KeyMappingHelper.registerKeyMapping(key.getKeyBinding().reference());
            });
        });

        ClientTickEvents.START_CLIENT_TICK.register(minecraft -> {
            getClientTickEvent().invoker().onClientTick(new MinecraftClient(minecraft), ClientTick.Phase.START);
        });

        ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
            getClientTickEvent().invoker().onClientTick(new MinecraftClient(minecraft), ClientTick.Phase.END);
            // 26.2 的 Fabric API 没有全局按键事件（只有屏幕内的 ScreenKeyboardEvents），
            // 而上游对按键的用法全是 KeyMapping.isDown() / consumeClick() 这种轮询语义，
            // 所以改成在 tick 末尾派发一次按键事件即可，不必再 mixin KeyboardHandler。
            getKeyInputEvent().invoker().onKeyInput(new InputKey(0, 0, 0, 0));
        });

        // 世界渲染：26.2 取消了 WorldRenderEvents，改为 fabric-rendering-v1 的 LevelRenderEvents。
        // 必须在 COLLECT_SUBMITS 阶段调用 —— 26.2 的自定义几何是通过
        // SubmitNodeCollector.submitCustomGeometry(...) 往收集器里塞节点的，
        // 提交时机不对的话节点不会进入渲染流程。
        LevelRenderEvents.COLLECT_SUBMITS.register(context -> {
            var partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
            getRenderWorldEvent().invoker().onRenderWorld(MinecraftRenderer.forWorld(context), partialTick);
        });

        // HUD 渲染：26.2 取消了 GuiMixin 的做法，官方提供 HudElementRegistry
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("mcoo_build_with_ease", "hud"), (graphics, deltaTracker) -> {
            getRenderGuiEvent().invoker().onRenderGui(MinecraftRenderer.forGui(graphics), deltaTracker.getGameTimeDeltaPartialTick(false));
        });

        InteractionInputEvents.ATTACK.register((player, hand) -> {
            return getInteractionInputEvent().invoker().onInteractionInput(InteractionType.ATTACK, MinecraftConvertor.fromPlatformInteractionHand(hand)).interruptsFurtherEvaluation();
        });

        InteractionInputEvents.USE_ITEM.register((player, hand) -> {
            return getInteractionInputEvent().invoker().onInteractionInput(InteractionType.USE_ITEM, MinecraftConvertor.fromPlatformInteractionHand(hand)).interruptsFurtherEvaluation();
        });

    }

}
