package com.mcoo.buildwithease.building.settings;

import java.util.Collections;
import java.util.List;

import com.mcoo.buildwithease.building.pattern.randomize.ItemRandomizer;

public record RandomizerSettings(
        List<ItemRandomizer> randomizers
) {

    public RandomizerSettings() {
        this(Collections.emptyList());
    }

}
