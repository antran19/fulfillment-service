package com.nexus.fulfillment.infrastructure.persistence.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory_reservations")
public class ReservationJpaEntity {

    @Id
    private UUID id;

    @Column(name = "reference_id", nullable = false)
    private String referenceId;

    @Column(name = "sku_id", nullable = false)
    private String skuId;

    @Column(name = "warehouse_id", nullable = false)
    private String warehouseId;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private String status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ReservationJpaEntity() {
    }

    public ReservationJpaEntity(UUID id, String referenceId, String skuId, String warehouseId, int quantity,
                                 String status, Instant expiresAt, Instant createdAt) {
        this.id = id;
        this.referenceId = referenceId;
        this.skuId = skuId;
        this.warehouseId = warehouseId;
        this.quantity = quantity;
        this.status = status;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public String getReferenceId() { return referenceId; }
    public String getSkuId() { return skuId; }
    public String getWarehouseId() { return warehouseId; }
    public int getQuantity() { return quantity; }
    public String getStatus() { return status; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
}
