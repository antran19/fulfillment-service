package com.nexus.fulfillment.domain.model;

import java.time.Instant;
import java.util.UUID;

// Immutable by design (SRS: "Ledger records shall not be modified once created") -- no
// with*/setter methods, no reconstitute-and-mutate path. Every movement to an
// InventoryRecord writes exactly one of these in the same transaction.
public class InventoryLedgerEntry {

    private final String id;
    private final LedgerMovementType movementType;
    private final String skuId;
    private final String warehouseId;
    private final int quantity;
    private final String referenceId;
    private final Instant occurredAt;

    private InventoryLedgerEntry(String id, LedgerMovementType movementType, String skuId, String warehouseId,
                                  int quantity, String referenceId, Instant occurredAt) {
        this.id = id;
        this.movementType = movementType;
        this.skuId = skuId;
        this.warehouseId = warehouseId;
        this.quantity = quantity;
        this.referenceId = referenceId;
        this.occurredAt = occurredAt;
    }

    public static InventoryLedgerEntry record(LedgerMovementType movementType, String skuId, String warehouseId,
                                               int quantity, String referenceId) {
        return new InventoryLedgerEntry(UUID.randomUUID().toString(), movementType, skuId, warehouseId, quantity,
                referenceId, Instant.now());
    }

    public static InventoryLedgerEntry reconstitute(String id, LedgerMovementType movementType, String skuId,
                                                      String warehouseId, int quantity, String referenceId,
                                                      Instant occurredAt) {
        return new InventoryLedgerEntry(id, movementType, skuId, warehouseId, quantity, referenceId, occurredAt);
    }

    public String getId() { return id; }
    public LedgerMovementType getMovementType() { return movementType; }
    public String getSkuId() { return skuId; }
    public String getWarehouseId() { return warehouseId; }
    public int getQuantity() { return quantity; }
    public String getReferenceId() { return referenceId; }
    public Instant getOccurredAt() { return occurredAt; }
}
