package dev.huskuraft.universal.api.core;

import dev.huskuraft.universal.api.platform.PlatformReference;

public interface DimensionType extends PlatformReference {

    boolean hasSkyLight();

    boolean hasCeiling();

    double coordinateScale();

    int minY();

    int height();

    int logicalHeight();


}
