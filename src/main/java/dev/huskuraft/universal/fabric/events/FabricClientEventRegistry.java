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
            // 「导出图片」的回读要提交命令编码器，必须在帧外做（帧内提交会让 Vulkan 掉设备）。
            // 物品图标图集的烘焙不在这里：它和原版 GuiItemAtlas / Axiom 一样，要放在
            // **GUI 提取阶段**（下面的 HUD 元素里）—— 那里没有活动 pass，且 RenderSystem 的
            // 投影/模型视图栈是 GUI 上下文，烘出来的图标才在正确位置（tick 里烘会吃到上一帧残留矩阵）。
            var exportRenderer = dev.huskuraft.universal.vanilla.renderer.preview.PreviewExportRenderer.get();
            if (exportRenderer != null) {
                exportRenderer.readBackIfReady();
            }
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
            var renderer = MinecraftRenderer.forWorld(context);
            getRenderWorldEvent().invoker().onRenderWorld(renderer, partialTick);
            // 【26.2 修正】世界侧的几何（选区框 / 图案 / 变换器预览）走 common-api 的顶点路径，
            // 26.2 必须显式 flush 才会进 GUI/世界的渲染状态 —— 不 flush 的话整段几何根本不会被画出来。
            renderer.flush();
        });

        // HUD 渲染：26.2 取消了 GuiMixin 的做法，官方提供 HudElementRegistry
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("mcoo_build_with_ease", "hud"), (graphics, deltaTracker) -> {
            // 物品图标烘焙：放在 GUI 提取阶段的最前面 —— 与 26.2 原版 GuiItemAtlas#drawToSlot
            // 和 Axiom 的 BlockRenderCache.renderTick 同一时机（没有活动 RenderPass）。
            // 这里只「立即绘制」到离屏图集，不往 GUI 渲染状态里加元素，所以不会影响后面的 onRenderGui。
            var iconAtlas = dev.huskuraft.universal.vanilla.gui.McooItemIconAtlas.ensure();
            if (iconAtlas != null) {
                try {
                    iconAtlas.flush();
                } catch (Throwable error) {
                    // 图标烘不出来不该拖累整个 HUD
                    dev.huskuraft.universal.vanilla.gui.McooItemIconAtlas.reportFailure(error);
                }
            }
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
