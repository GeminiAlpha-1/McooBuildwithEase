package com.mcoo.buildwithease.screen.common;

import com.mcoo.buildwithease.McooBuildwithEaseClient;
import dev.huskuraft.universal.api.gui.AbstractPanelScreen;
import dev.huskuraft.universal.api.platform.Entrance;
import dev.huskuraft.universal.api.text.Text;

public abstract class McooBuildwithEasePanelScreen extends AbstractPanelScreen {
    protected McooBuildwithEasePanelScreen(Entrance entrance, Text title) {
        super(entrance, title);
    }



    @Override
    protected McooBuildwithEaseClient getEntrance() {
        return (McooBuildwithEaseClient) super.getEntrance();
    }
}
