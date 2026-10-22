package com.mcoo.buildwithease.building.operation;

import com.mcoo.buildwithease.building.pattern.RefactorContext;

public interface Refactorable<O> extends Trait {

    O refactor(RefactorContext source);

}
