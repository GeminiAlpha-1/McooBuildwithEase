package com.mcoo.buildwithease.screen.common;

import com.mcoo.buildwithease.McooBuildwithEaseClient;
import dev.huskuraft.universal.api.gui.AbstractScreen;
import dev.huskuraft.universal.api.platform.Entrance;
import dev.huskuraft.universal.api.text.Text;

public abstract class McooBuildwithEaseScreen extends AbstractScreen {
    protected McooBuildwithEaseScreen(Entrance entrance, Text title) {
        super(entrance, title);
    }

    @Override
    protected McooBuildwithEaseClient getEntrance() {
        return (McooBuildwithEaseClient) super.getEntrance();
    }
}
