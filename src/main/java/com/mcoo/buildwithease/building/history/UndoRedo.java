package com.mcoo.buildwithease.building.history;

import com.mcoo.buildwithease.building.Option;

public enum UndoRedo implements Option {
    UNDO("undo"),
    REDO("redo"),
    ;

    private final String name;

    UndoRedo(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getCategory() {
        return "history";
    }

}
