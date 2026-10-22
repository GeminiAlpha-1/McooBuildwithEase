package com.mcoo.buildwithease.building.pattern;

import com.mcoo.buildwithease.building.Option;

public enum Patterns implements Option {
    DISABLED("pattern_disabled"),
    ENABLED("pattern_enabled")
    ;

    private final String name;

    Patterns(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getCategory() {
        return "pattern";
    }

}
