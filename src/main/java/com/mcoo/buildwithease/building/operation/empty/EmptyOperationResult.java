package com.mcoo.buildwithease.building.operation.empty;

import java.util.List;

import dev.huskuraft.universal.api.core.ItemStack;
import com.mcoo.buildwithease.building.operation.ItemSummary;
import com.mcoo.buildwithease.building.operation.Operation;
import com.mcoo.buildwithease.building.operation.OperationResult;

public class EmptyOperationResult extends OperationResult {

    private final EmptyOperation operation;

    public EmptyOperationResult(EmptyOperation operation) {

        this.operation = operation;
    }

    @Override
    public Operation getOperation() {
        return operation;
    }

    @Override
    public EmptyOperation getReverseOperation() {
        return operation;
    }

    @Override
    public int getAffectedBlockCount() {
        return 0;
    }

    @Override
    public List<ItemStack> getItemSummary(ItemSummary itemSummary) {
        return List.of();
    }

}
