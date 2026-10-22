package dev.huskuraft.universal.api.plugin.openpac;

import dev.huskuraft.universal.api.platform.ClientPlugin;

public interface OpenPacClientPlugin extends ClientPlugin {

    OpenPacChunkClaimsManager getClaimManager();

}
