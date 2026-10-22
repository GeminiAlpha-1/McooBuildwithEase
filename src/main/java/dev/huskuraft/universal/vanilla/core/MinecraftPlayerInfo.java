package dev.huskuraft.universal.vanilla.core;

import dev.huskuraft.universal.api.core.PlayerInfo;
import dev.huskuraft.universal.api.core.PlayerProfile;
import dev.huskuraft.universal.api.core.PlayerSkin;
import dev.huskuraft.universal.api.text.Text;

public record MinecraftPlayerInfo(
        net.minecraft.client.multiplayer.PlayerInfo refs
) implements PlayerInfo {

    @Override
    public PlayerProfile getProfile() {
        return new MinecraftPlayerProfile(refs.getProfile());
    }

    @Override
    public Text getDisplayName() {
        return MinecraftText.ofNullable(refs.getTabListDisplayName());
    }

    /**
     * 26.2 起 PlayerSkin 挪到了公共包 {@code net.minecraft.world.entity.player.PlayerSkin}，
     * 三个贴图字段改名为 body() / cape() / elytra()，类型是 {@code ClientAsset.Texture}，
     * 资源位置要通过 {@code texturePath()} 取。
     */
    @Override
    public PlayerSkin getSkin() {
        var skin = refs.getSkin();
        return new PlayerSkin(
                MinecraftResourceLocation.ofNullable(skin.body() == null ? null : skin.body().texturePath()),
                MinecraftResourceLocation.ofNullable(skin.cape() == null ? null : skin.cape().texturePath()),
                MinecraftResourceLocation.ofNullable(skin.elytra() == null ? null : skin.elytra().texturePath()),
                switch (skin.model()) {
                    case SLIM -> PlayerSkin.Model.SLIM;
                    case WIDE -> PlayerSkin.Model.WIDE;
                }
        );
    }
}
