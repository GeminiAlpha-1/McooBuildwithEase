package dev.huskuraft.universal.vanilla.core;

import dev.huskuraft.universal.api.core.PlayerProfile;
import dev.huskuraft.universal.api.platform.PlayerList;
import dev.huskuraft.universal.api.platform.Server;

public record MinecraftServer(
        net.minecraft.server.MinecraftServer refs
) implements Server {

    @Override
    public PlayerList getPlayerList() {
        return new MinecraftPlayerList(refs.getPlayerList());
    }

    @Override
    public void execute(Runnable runnable) {
        refs.execute(runnable);
    }

    @Override
    public boolean isSinglePlayerOwner(PlayerProfile profile) {
        // 【26.2 修正】26.2 的 MinecraftServer#isSingleplayerOwner 收的是 NameAndId（id + 名字的记录），
        // 直接把 GameProfile 传过去会抛 ClassCastException，
        // 服务端处理「权限校验」包的任务直接失败 → 回包发不出去 → 通用设置点了没反应。
        return refs.isSingleplayerOwner(new net.minecraft.server.players.NameAndId(profile.getId(), profile.getName()));
    }

    @Override
    public boolean isDedicatedServer() {
        return refs.isDedicatedServer();
    }

}
