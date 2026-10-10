package com.nexus.fulfillment.application.usecase;

import com.nexus.fulfillment.application.port.out.InventoryRecordRepositoryPort;
import com.nexus.fulfillment.domain.model.InventoryRecord;

public class GetInventoryUseCase {

    private final InventoryRecordRepositoryPort inventoryRecordRepositoryPort;

    public GetInventoryUseCase(InventoryRecordRepositoryPort inventoryRecordRepositoryPort) {
        this.inventoryRecordRepositoryPort = inventoryRecordRepositoryPort;
    }

    // A SKU with no intake yet is zero stock, not a 404 -- same lazy-default convention as
    // GetReputationUseCase in user-service.
    public InventoryRecord get(String skuId, String warehouseId) {
        return inventoryRecordRepositoryPort.findBySkuAndWarehouse(skuId, warehouseId)
                .orElseGet(() -> InventoryRecord.createEmpty(skuId, warehouseId));
    }
}
