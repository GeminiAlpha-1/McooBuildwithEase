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
 * <p><b>注入点必须与上游 1.21.3 一致，不能改成方法头。</b>
 * 26.2 里这些方法都改成了 private：
 * {@code startAttack()} / {@code continueAttack(boolean)} / {@code startUseItem()} / {@code pickBlockOrEntity()}
 * （1.21.3 的 {@code pickBlock} 在 26.2 改名为 {@code pickBlockOrEntity}）。
 *
 * <p>为什么不能用 {@code @At("HEAD")}：
 * 上游把事件挂在「真的在攻击 / 真的在用物品」的具体调用点上，
 * 事件因此只在**确实存在目标**时触发一次。
 * 改到方法头之后，事件会在「按住鼠标的每一 tick」都触发，
 * 而业务层的 {@code Interactions.put()} 是纯追加不去重，
 * 交互列表会无限增长 → 客户端每 tick 都把越来越大的交互集合发给服务端 →
 * 服务端每 tick 破坏 N 个方块（N 还在涨）→ 世界被瞬间铲平。
 * 26.2 字节码里这四个锚点都还在（已逐一核对），所以按上游原样挂回去。
 */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Shadow
    @Nullable
    public LocalPlayer player;

    /**
     * 点击瞬间开始攻击。锚点 = {@code HitResult#getType()}，
     * 即「射线确实打到了什么东西」时才通知业务层（与上游 1.21.3 一致）。
     */
    @Inject(method = "startAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/HitResult;getType()Lnet/minecraft/world/phys/HitResult$Type;"), cancellable = true)
    private void onPlayerStartAttack(CallbackInfoReturnable<Boolean> cir) {
        if (InteractionInputEvents.ATTACK.invoker().onAttack(player, InteractionHand.MAIN_HAND)) {
            cir.setReturnValue(false);
        }
    }

    /**
     * 按住鼠标持续破坏。锚点 = {@code BlockState#isAir()}，
     * 即「确实在破坏一个非空气方块」时才通知业务层（与上游 1.21.3 一致）。
     */
    @Inject(method = "continueAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;isAir()Z"), cancellable = true)
    private void onPlayerContinueAttack(boolean bl, CallbackInfo ci) {
        if (InteractionInputEvents.ATTACK.invoker().onAttack(player, InteractionHand.MAIN_HAND)) {
            ci.cancel();
        }
    }

    /**
     * 使用物品。锚点 = {@code LocalPlayer#getItemInHand(InteractionHand)}（与上游 1.21.3 一致）。
     */
    @Inject(method = "startUseItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getItemInHand(Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/item/ItemStack;"), cancellable = true)
    private void onPlayerStartUseItem(CallbackInfo ci) {
        if (InteractionInputEvents.USE_ITEM.invoker().onUseItem(player, InteractionHand.MAIN_HAND)) {
            ci.cancel();
        }
    }

    /**
     * 中键选取方块。
     * <p>
     * 26.2 里 {@code pickBlockOrEntity} 内部已经不再调用 {@code LocalPlayer#getAbilities()}
     * （上游 1.21.3 的锚点在 26.2 不存在，会导致 mixin 注入失败直接崩客户端），
     * 改用同样稳定、且已在 {@code startAttack} 验证过的 {@code HitResult#getType()} 锚点：
     * 语义是「射线确实打到了什么东西」，与「选取方块」的意图一致。
     */
    @Inject(method = "pickBlockOrEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/HitResult;getType()Lnet/minecraft/world/phys/HitResult$Type;"), cancellable = true)
    private void onPlayerPickBlock(CallbackInfo ci) {
        if (InteractionInputEvents.PICK_BLOCK.invoker().onPickBlock(player, InteractionHand.MAIN_HAND)) {
            ci.cancel();
        }
    }

}
