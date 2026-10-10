package com.nexus.fulfillment.application.exception;

import com.nexus.common.core.exception.ConflictException;

public class InsufficientInventoryException extends ConflictException {
    public InsufficientInventoryException(String skuId, String warehouseId, int quantity) {
        super("INSUFFICIENT_INVENTORY",
                "Not enough available stock for SKU " + skuId + " in warehouse " + warehouseId
                        + " to reserve " + quantity);
    }
}
