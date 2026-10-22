package com.mcoo.buildwithease.building.operation;

import com.mcoo.buildwithease.building.Context;

public interface Operation extends Mirrorable<Operation>, Movable<Operation>, Rotatable<Operation>, Refactorable<Operation> {

    Context getContext();

    OperationResult commit();

}
