package com.nexus.fulfillment.application.port.out;

import com.nexus.fulfillment.domain.model.InventoryRecord;

import java.util.Optional;

public interface InventoryRecordRepositoryPort {

    Optional<InventoryRecord> findBySkuAndWarehouse(String skuId, String warehouseId);

    // Atomic upsert: creates the row at `quantity` if it doesn't exist yet, otherwise adds
    // `quantity` to the existing total. Race-free under concurrent intake for a brand-new SKU.
    void intake(String skuId, String warehouseId, int quantity);

    // Atomically increments reservedQuantity by `quantity` IFF availableQuantity >= quantity,
    // in one conditional UPDATE -- never via load-mutate-save. Returns true if applied.
    boolean tryReserve(String skuId, String warehouseId, int quantity);

    // Returns a previously reserved quantity to available stock (reservedQuantity -= quantity).
    void release(String skuId, String warehouseId, int quantity);

    // Permanently removes a previously reserved quantity from stock
    // (totalQuantity -= quantity, reservedQuantity -= quantity).
    void commit(String skuId, String warehouseId, int quantity);
}
