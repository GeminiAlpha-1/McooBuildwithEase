package com.mcoo.buildwithease.building.structure;

import com.mcoo.buildwithease.building.SingleSelectFeature;

public interface BuildFeature extends SingleSelectFeature {

    @Override
    default String getCategory() {
        return getType().getName();
    }

    BuildFeatures getType();

}
