package com.mcoo.buildwithease.building;

import dev.huskuraft.universal.api.text.Text;

public enum PositionType {
    ABSOLUTE("absolute"),
    RELATIVE("relative");

    private final String name;

    PositionType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Text getDisplayName() {
        return Text.translate("mcoo_build_with_ease.position.%s".formatted(name));
    }

}
