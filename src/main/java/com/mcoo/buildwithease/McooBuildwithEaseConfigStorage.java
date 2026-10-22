package com.mcoo.buildwithease;

import dev.huskuraft.universal.api.file.ConfigFileStorage;
import dev.huskuraft.universal.api.file.FileType;
import com.mcoo.buildwithease.session.config.SessionConfig;
import com.mcoo.buildwithease.session.config.serializer.SessionConfigSerializer;

public final class McooBuildwithEaseConfigStorage extends ConfigFileStorage<SessionConfig> {

    public static final String CONFIG_NAME = "mcoo_build_with_ease.toml";

    public McooBuildwithEaseConfigStorage(McooBuildwithEase entrance) {
        super(CONFIG_NAME, FileType.TOML, new SessionConfigSerializer());
    }

}
