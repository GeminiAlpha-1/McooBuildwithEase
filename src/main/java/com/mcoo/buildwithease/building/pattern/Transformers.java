package com.mcoo.buildwithease.building.pattern;

import dev.huskuraft.universal.api.text.Text;

public enum Transformers {
    ARRAY("array"),
    MIRROR("mirror"),
    RADIAL("radial"),
    RANDOMIZER("randomizer");

    private final String name;

    Transformers(String name) {
        this.name = name;
    }

    public Text getDisplayName() {
        return Text.translate("mcoo_build_with_ease.transformer.%s".formatted(name));
    }

    public Text getTitleText() {
        return Text.translate("mcoo_build_with_ease.transformer.%s.title".formatted(name));
    }

}
