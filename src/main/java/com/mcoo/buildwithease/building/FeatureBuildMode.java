package com.mcoo.buildwithease.building;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import com.mcoo.buildwithease.building.structure.BuildFeature;
import com.mcoo.buildwithease.building.structure.BuildFeatures;
import com.mcoo.buildwithease.building.structure.BuildMode;

record FeatureBuildMode(
        BuildMode buildMode,
        Set<BuildFeature> features
) {

    public FeatureBuildMode {

    }

    public static FeatureBuildMode createDefault(BuildMode buildMode) {
        return new FeatureBuildMode(buildMode, Arrays.stream(buildMode.getSupportedFeatures()).map(BuildFeatures::getDefaultFeature).collect(Collectors.toSet()));
    }


    public FeatureBuildMode withBuildFeature(BuildFeature feature) {
        return this;
    }


}
