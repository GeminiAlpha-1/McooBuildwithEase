package com.mcoo.buildwithease.building.operation;

import com.mcoo.buildwithease.building.pattern.MirrorContext;

public interface Mirrorable<O> extends Trait {

    O mirror(MirrorContext mirrorContext);

}
