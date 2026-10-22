package com.mcoo.buildwithease;

import dev.huskuraft.universal.api.file.FileType;
import dev.huskuraft.universal.api.file.TagElementFileStorage;
import com.mcoo.buildwithease.building.config.ClientConfig;
import com.mcoo.buildwithease.building.config.tag.ClientConfigTagSerializer;

public final class McooBuildwithEaseClientTagConfigStorage extends TagElementFileStorage<ClientConfig> {

    public McooBuildwithEaseClientTagConfigStorage(McooBuildwithEaseClient entrance) {
        super("mcoo_build_with_ease-client.dat", FileType.NBT, new ClientConfigTagSerializer());
    }

    @Override
    public ClientConfig getDefault() {
        return ClientConfig.DEFAULT;
    }

}
