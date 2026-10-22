package dev.huskuraft.universal.api.plugin.ftbchunks;

import dev.huskuraft.universal.api.platform.PlatformReference;

import java.util.UUID;

public interface FtbClaimedChunk extends PlatformReference {

    boolean isTeamMember(UUID uuid);

}
