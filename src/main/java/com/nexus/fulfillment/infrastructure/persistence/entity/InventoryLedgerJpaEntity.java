package com.nexus.fulfillment.infrastructure.persistence.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory_ledger")
public class InventoryLedgerJpaEntity {

    @Id
    private UUID id;

    @Column(name = "sku_id", nullable = false)
    private String skuId;

    @Column(name = "warehouse_id", nullable = false)
    private String warehouseId;

    @Column(name = "movement_type", nullable = false)
    private String movementType;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "reference_id")
    private String referenceId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected InventoryLedgerJpaEntity() {
    }

    public InventoryLedgerJpaEntity(UUID id, String skuId, String warehouseId, String movementType, int quantity,
                                     String referenceId, Instant occurredAt) {
        this.id = id;
        this.skuId = skuId;
        this.warehouseId = warehouseId;
        this.movementType = movementType;
        this.quantity = quantity;
        this.referenceId = referenceId;
        this.occurredAt = occurredAt;
    }

    public UUID getId() { return id; }
    public String getSkuId() { return skuId; }
    public String getWarehouseId() { return warehouseId; }
    public String getMovementType() { return movementType; }
    public int getQuantity() { return quantity; }
    public String getReferenceId() { return referenceId; }
    public Instant getOccurredAt() { return occurredAt; }
}
