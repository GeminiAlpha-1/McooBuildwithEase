package dev.huskuraft.universal.fabric.mixin;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.huskuraft.universal.fabric.events.common.InteractionInputEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;

/**
 * 客户端输入拦截。
 *
 * <p>26.2 的 Minecraft 里这几个方法都改成了 private：
 * {@code startAttack()} / {@code continueAttack(boolean)} / {@code startUseItem()} / {@code pickBlockOrEntity()}
 * （1.21.3 的 {@code pickBlock} 在 26.2 改名为 {@code pickBlockOrEntity}）。
 *
 * <p>注入点一律放在方法 HEAD：不依赖方法内部的调用序列，比上游 1.21.3 那种
 * {@code @At("INVOKE", target = "...HitResult;getType()")} 的深层锚点稳定得多，跟版时只需确认方法名还在。
 */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Shadow
    @Nullable
    public LocalPlayer player;

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void onPlayerStartAttack(CallbackInfoReturnable<Boolean> cir) {
        if (InteractionInputEvents.ATTACK.invoker().onAttack(player, InteractionHand.MAIN_HAND)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void onPlayerContinueAttack(boolean bl, CallbackInfo ci) {
        if (InteractionInputEvents.ATTACK.invoker().onAttack(player, InteractionHand.MAIN_HAND)) {
            ci.cancel();
        }
    }

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void onPlayerStartUseItem(CallbackInfo ci) {
        if (InteractionInputEvents.USE_ITEM.invoker().onUseItem(player, InteractionHand.MAIN_HAND)) {
            ci.cancel();
        }
    }

    @Inject(method = "pickBlockOrEntity", at = @At("HEAD"), cancellable = true)
    private void onPlayerPickBlock(CallbackInfo ci) {
        if (InteractionInputEvents.PICK_BLOCK.invoker().onPickBlock(player, InteractionHand.MAIN_HAND)) {
            ci.cancel();
        }
    }

}
