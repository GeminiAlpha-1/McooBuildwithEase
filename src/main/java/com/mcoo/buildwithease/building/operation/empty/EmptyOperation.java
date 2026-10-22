package com.mcoo.buildwithease.building.operation.empty;

import com.mcoo.buildwithease.building.Context;
import com.mcoo.buildwithease.building.operation.Operation;
import com.mcoo.buildwithease.building.operation.OperationResult;
import com.mcoo.buildwithease.building.pattern.MirrorContext;
import com.mcoo.buildwithease.building.pattern.MoveContext;
import com.mcoo.buildwithease.building.pattern.RefactorContext;
import com.mcoo.buildwithease.building.pattern.RotateContext;

public final class EmptyOperation implements Operation {

    private final Context context;

    public EmptyOperation(Context context) {
        this.context = context;
    }

    @Override
    public Operation mirror(MirrorContext mirrorContext) {
        return this;
    }

    @Override
    public Operation move(MoveContext moveContext) {
        return this;
    }

    @Override
    public Context getContext() {
        return context;
    }

    @Override
    public OperationResult commit() {
        return new EmptyOperationResult(this);
    }

    @Override
    public Operation refactor(RefactorContext source) {
        return this;
    }

    @Override
    public Operation rotate(RotateContext rotateContext) {
        return this;
    }
}
