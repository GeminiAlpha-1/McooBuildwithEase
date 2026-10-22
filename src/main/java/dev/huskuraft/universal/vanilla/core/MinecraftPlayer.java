package dev.huskuraft.universal.vanilla.core;

import dev.huskuraft.universal.api.core.BlockInteraction;
import dev.huskuraft.universal.api.core.GameMode;
import dev.huskuraft.universal.api.core.InteractionHand;
import dev.huskuraft.universal.api.core.Inventory;
import dev.huskuraft.universal.api.core.ItemStack;
import dev.huskuraft.universal.api.core.Player;
import dev.huskuraft.universal.api.core.PlayerProfile;
import dev.huskuraft.universal.api.core.Stat;
import dev.huskuraft.universal.api.core.World;
import dev.huskuraft.universal.api.math.Vector3d;
import dev.huskuraft.universal.api.platform.Client;
import dev.huskuraft.universal.api.platform.Server;
import dev.huskuraft.universal.api.text.Text;
import dev.huskuraft.universal.vanilla.platform.MinecraftClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public record MinecraftPlayer(net.minecraft.world.entity.player.Player refs) implements Player {

    public static Player ofNullable(net.minecraft.world.entity.player.Player refs) {
        if (refs == null) return null;
        return new MinecraftPlayer(refs);
    }

    @Override
    public UUID getId() {
        return refs.getUUID();
    }

    @Override
    public PlayerProfile getProfile() {
        return new MinecraftPlayerProfile(refs.getGameProfile());
    }

    @Override
    public Inventory getInventory() {
        return new MinecraftInventory(refs.getInventory());
    }

    @Override
    public boolean isDeadOrDying() {
        return refs.isDeadOrDying();
    }

    @Override
    public Client getClient() {
        return new MinecraftClient(Minecraft.getInstance());
    }

    @Override
    public Server getServer() {
        // 26.2：Entity 上没有 getServer() 了，从所在世界拿
        var level = refs.level();
        if (level == null || level.getServer() == null) {
            return null;
        }
        return new MinecraftServer(level.getServer());
    }

    @Override
    public World getWorld() {
        return MinecraftWorld.ofNullable(refs.level());
    }

    @Override
    public Text getDisplayName() {
        return new MinecraftText(refs.getDisplayName());
    }

    @Override
    public Vector3d getPosition() {
        return MinecraftConvertor.fromPlatformVector3d(refs.position());
    }

    @Override
    public void drop(ItemStack itemStack, boolean dropAround, boolean includeThrowerName) {
        refs.drop(itemStack.reference(), dropAround, includeThrowerName);
    }

    @Override
    public void sendMessage(Text message, boolean actionBar) {
        // 26.2：displayClientMessage(Component, boolean) 拆成了 sendSystemMessage / sendOverlayMessage
        if (actionBar) {
            refs.sendOverlayMessage(message.reference());
        } else {
            refs.sendSystemMessage(message.reference());
        }
    }

    @Override
    public void swing(InteractionHand hand) {
        refs.swing(MinecraftConvertor.toPlatformInteractionHand(hand));
    }

    @Override
    public GameMode getGameMode() {
        if (refs instanceof ServerPlayer serverPlayer) {
            return switch (serverPlayer.gameMode.getGameModeForPlayer()) {
                case SURVIVAL -> GameMode.SURVIVAL;
                case CREATIVE -> GameMode.CREATIVE;
                case ADVENTURE -> GameMode.ADVENTURE;
                case SPECTATOR -> GameMode.SPECTATOR;
            };
        }
        if (refs instanceof AbstractClientPlayer localPlayer) {
            // 26.2：AbstractClientPlayer#getPlayerInfo() 是 protected，
            // 改从客户端连接上按 UUID 取
            var connection = Minecraft.getInstance().getConnection();
            var playerInfo = connection == null ? null : connection.getPlayerInfo(localPlayer.getUUID());
            if (playerInfo == null) {
                return null;
            }
            return switch (playerInfo.getGameMode()) {
                case SURVIVAL -> GameMode.SURVIVAL;
                case CREATIVE -> GameMode.CREATIVE;
                case ADVENTURE -> GameMode.ADVENTURE;
                case SPECTATOR -> GameMode.SPECTATOR;
            };
        }
        return null;
    }

    @Override
    public BlockInteraction raytrace(double maxDistance, float deltaTick, boolean includeFluids) {
        return (BlockInteraction) MinecraftConvertor.fromPlatformInteraction(refs.pick(maxDistance, deltaTick, includeFluids));
    }

    @Override
    public float getEyeHeight() {
        return refs.getEyeHeight();
    }

    @Override
    public void setPosition(Vector3d position) {
        refs.setPos(MinecraftConvertor.toPlatformVector3d(position));
    }

    @Override
    public float getXRot() {
        return refs.getXRot();
    }

    @Override
    public void setXRot(float xRot) {
        refs.setXRot(xRot);
    }

    @Override
    public float getYRot() {
        return refs.getYRot();
    }

    @Override
    public void setYRot(float yRot) {
        refs.setYRot(yRot);
    }

    @Override
    public void awardStat(Stat<?> stat, int increment) {
        refs.awardStat((net.minecraft.stats.Stat<?>) stat.reference(), increment);
    }

    @Override
    public void resetStat(Stat<?> stat) {
        refs.resetStat(stat.reference());
    }

}
