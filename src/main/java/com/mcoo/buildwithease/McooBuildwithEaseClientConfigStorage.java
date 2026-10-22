package com.mcoo.buildwithease;

import dev.huskuraft.universal.api.file.ConfigFileStorage;
import dev.huskuraft.universal.api.file.FileType;
import com.mcoo.buildwithease.building.config.ClientConfig;
import com.mcoo.buildwithease.building.config.universal.ClientConfigConfigSerializer;
import com.mcoo.buildwithease.building.structure.BuildMode;
import com.mcoo.buildwithease.building.structure.builder.Structure;

public final class McooBuildwithEaseClientConfigStorage extends ConfigFileStorage<ClientConfig> {

    public static final String CONFIG_NAME = "mcoo_build_with_ease-client.toml";

    public McooBuildwithEaseClientConfigStorage(McooBuildwithEaseClient entrance) {
        super(CONFIG_NAME, FileType.TOML, new ClientConfigConfigSerializer());
    }

    public void setStructure(Structure structure) {
        set(get().withStructure(structure));
    }

    public Structure getStructure(BuildMode buildMode) {
        return get().getStructure(buildMode);
    }

}
