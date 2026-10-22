package com.mcoo.buildwithease.building.operation.batch;

import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import com.mcoo.buildwithease.building.Context;
import com.mcoo.buildwithease.building.operation.Operation;
import com.mcoo.buildwithease.building.pattern.MirrorContext;
import com.mcoo.buildwithease.building.pattern.MoveContext;
import com.mcoo.buildwithease.building.pattern.RefactorContext;
import com.mcoo.buildwithease.building.pattern.RotateContext;

public class DeferredBatchOperation extends BatchOperation {

    protected final Supplier<Stream<? extends Operation>> operationsSupplier;

    public DeferredBatchOperation(Context context, Supplier<Stream<? extends Operation>> operationsSupplier) {
        super(context);
        this.operationsSupplier = operationsSupplier;
    }

    @Override
    public BatchOperationResult commit() {
        return new BatchOperationResult(this, operations().map(Operation::commit).toList());
    }

    @Override
    public DeferredBatchOperation move(MoveContext moveContext) {
        return new DeferredBatchOperation(context, () -> operations().map(o -> o.move(moveContext)));
    }

    @Override
    public DeferredBatchOperation mirror(MirrorContext mirrorContext) {
        return new DeferredBatchOperation(context, () -> operations().map(o -> o.mirror(mirrorContext)));
    }

    @Override
    public DeferredBatchOperation rotate(RotateContext rotateContext) {
        return new DeferredBatchOperation(context, () -> operations().map(o -> o.rotate(rotateContext)));
    }

    @Override
    public DeferredBatchOperation refactor(RefactorContext source) {
        return new DeferredBatchOperation(context, () -> operations().map(o -> o.refactor(source)));
    }

    @Override
    public DeferredBatchOperation map(UnaryOperator<Operation> operator) {
        return new DeferredBatchOperation(context, () -> operations().map(operator));
    }

    @Override
    public DeferredBatchOperation mapEach(UnaryOperator<Operation> operator) {
        return new DeferredBatchOperation(context, () -> operations().map(op -> {
            if (op instanceof BatchOperation op1) {
                return op1.mapEach(operator);
            } else {
                return operator.apply(op);
            }
        }));
    }

    @Override
    public DeferredBatchOperation flatten() {
        return new DeferredBatchOperation(context, () -> {
            return operations().flatMap(op -> op instanceof BatchOperation op1 ? op1.flatten().operations() : Stream.of(op));
        });
    }

    @Override
    public DeferredBatchOperation filter(Predicate<Operation> predicate) {
        return new DeferredBatchOperation(context, () -> operations().filter(predicate));
    }

    @Override
    public Stream<? extends Operation> operations() {
        return operationsSupplier.get();
    }

}
