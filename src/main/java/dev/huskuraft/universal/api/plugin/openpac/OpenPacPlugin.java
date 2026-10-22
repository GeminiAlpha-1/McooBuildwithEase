package dev.huskuraft.universal.api.plugin.openpac;

import javax.annotation.Nonnull;

import dev.huskuraft.universal.api.platform.Plugin;
import dev.huskuraft.universal.api.platform.Server;

public interface OpenPacPlugin extends Plugin {

    OpenPacChunkClaimsManager getServerClaimManager(@Nonnull Server server);

}
