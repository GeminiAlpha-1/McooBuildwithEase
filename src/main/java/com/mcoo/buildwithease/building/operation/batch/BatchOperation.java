package com.mcoo.buildwithease.building.operation.batch;

import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import com.mcoo.buildwithease.building.Context;
import com.mcoo.buildwithease.building.operation.Operation;

public abstract class BatchOperation implements Operation {

    protected final Context context;

    protected BatchOperation(Context context) {
        this.context = context;
    }

    public Context getContext() {
        return context;
    }

    @Override
    public abstract BatchOperationResult commit();

    public abstract BatchOperation map(UnaryOperator<Operation> operator);

    public abstract BatchOperation mapEach(UnaryOperator<Operation> operator);

    public abstract BatchOperation flatten();

    public abstract BatchOperation filter(Predicate<Operation> predicate);

    public abstract Stream<? extends Operation> operations();

}
