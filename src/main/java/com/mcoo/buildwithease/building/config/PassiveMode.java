package com.mcoo.buildwithease.building.config;

import com.mcoo.buildwithease.building.SingleSelectFeature;

public enum PassiveMode implements SingleSelectFeature {
    DISABLED("passive_mode_disabled"),
    ENABLED("passive_mode_enabled");

    private final String name;

    PassiveMode(String name) {
        this.name = name;
    }


    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getCategory() {
        return "passive_mode";
    }

}
