package dev.huskuraft.universal.api.core.fluid;

import dev.huskuraft.universal.api.platform.ContentFactory;

public enum Fluids {
    EMPTY,
    FLOWING_WATER,
    WATER,
    FLOWING_LAVA,
    LAVA,
    ;

    public Fluid fluid() {
        return ContentFactory.getInstance().getFluid(this);
    }

}
