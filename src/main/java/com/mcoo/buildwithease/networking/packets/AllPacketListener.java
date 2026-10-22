package com.mcoo.buildwithease.networking.packets;

import dev.huskuraft.universal.api.core.Player;
import dev.huskuraft.universal.api.networking.PacketListener;
import com.mcoo.buildwithease.networking.packets.player.PlayerBuildPacket;
import com.mcoo.buildwithease.networking.packets.player.PlayerBuildTooltipPacket;
import com.mcoo.buildwithease.networking.packets.player.PlayerCommandPacket;
import com.mcoo.buildwithease.networking.packets.player.PlayerPermissionCheckPacket;
import com.mcoo.buildwithease.networking.packets.player.PlayerSettingsPacket;
import com.mcoo.buildwithease.networking.packets.player.PlayerSnapshotCapturePacket;
import com.mcoo.buildwithease.networking.packets.player.PlayerSnapshotSharePacket;
import com.mcoo.buildwithease.networking.packets.session.SessionConfigPacket;
import com.mcoo.buildwithease.networking.packets.session.SessionPacket;

public interface AllPacketListener extends PacketListener {

    void handle(PlayerCommandPacket packet, Player player);

    void handle(PlayerSettingsPacket packet, Player player);

    void handle(PlayerBuildPacket packet, Player player);

    void handle(PlayerPermissionCheckPacket packet, Player player);

    void handle(PlayerBuildTooltipPacket packet, Player player);

    void handle(SessionPacket packet, Player player);

    void handle(SessionConfigPacket packet, Player player);

    void handle(PlayerSnapshotCapturePacket packet, Player player);

    void handle(PlayerSnapshotSharePacket packet, Player player);

}
