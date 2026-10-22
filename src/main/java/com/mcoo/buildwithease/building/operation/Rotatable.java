package com.mcoo.buildwithease.building.operation;

import com.mcoo.buildwithease.building.pattern.RotateContext;

public interface Rotatable<O> extends Trait {

    O rotate(RotateContext rotateContext);

}
