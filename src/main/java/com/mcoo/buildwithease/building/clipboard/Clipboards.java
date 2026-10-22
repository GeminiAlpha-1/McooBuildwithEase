package com.mcoo.buildwithease.building.clipboard;

import com.mcoo.buildwithease.building.Option;

public enum Clipboards implements Option {
    DISABLED("clipboard_disabled"),
    ENABLED("clipboard_enabled")
    ;

    private final String name;

    Clipboards(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getCategory() {
        return "clipboard";
    }

}
