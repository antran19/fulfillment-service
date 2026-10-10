package com.nexus.fulfillment.infrastructure.persistence.entity;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "inventory_records")
public class InventoryRecordJpaEntity {

    @Id
    private UUID id;

    @Column(name = "sku_id", nullable = false)
    private String skuId;

    @Column(name = "warehouse_id", nullable = false)
    private String warehouseId;

    @Column(name = "total_quantity", nullable = false)
    private int totalQuantity;

    @Column(name = "reserved_quantity", nullable = false)
    private int reservedQuantity;

    @Column(name = "unavailable_quantity", nullable = false)
    private int unavailableQuantity;

    protected InventoryRecordJpaEntity() {
    }

    public InventoryRecordJpaEntity(UUID id, String skuId, String warehouseId, int totalQuantity,
                                     int reservedQuantity, int unavailableQuantity) {
        this.id = id;
        this.skuId = skuId;
        this.warehouseId = warehouseId;
        this.totalQuantity = totalQuantity;
        this.reservedQuantity = reservedQuantity;
        this.unavailableQuantity = unavailableQuantity;
    }

    public UUID getId() { return id; }
    public String getSkuId() { return skuId; }
    public String getWarehouseId() { return warehouseId; }
    public int getTotalQuantity() { return totalQuantity; }
    public int getReservedQuantity() { return reservedQuantity; }
    public int getUnavailableQuantity() { return unavailableQuantity; }
}
