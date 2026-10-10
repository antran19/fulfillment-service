package com.nexus.fulfillment.domain.model;

// Read-model view of a SKU+warehouse's stock. The actual reserve/release/commit math is
// NEVER done by loading this, mutating it, and saving it back (that's exactly the
// read-modify-write race this whole service exists to avoid) -- it's done with atomic
// conditional UPDATE statements in InventoryRecordRepositoryPort. This class is for
// reading the current state only.
public class InventoryRecord {

    private final String skuId;
    private final String warehouseId;
    private final int totalQuantity;
    private final int reservedQuantity;
    private final int unavailableQuantity;

    private InventoryRecord(String skuId, String warehouseId, int totalQuantity, int reservedQuantity,
                             int unavailableQuantity) {
        this.skuId = skuId;
        this.warehouseId = warehouseId;
        this.totalQuantity = totalQuantity;
        this.reservedQuantity = reservedQuantity;
        this.unavailableQuantity = unavailableQuantity;
    }

    public static InventoryRecord createEmpty(String skuId, String warehouseId) {
        return new InventoryRecord(skuId, warehouseId, 0, 0, 0);
    }

    public static InventoryRecord reconstitute(String skuId, String warehouseId, int totalQuantity,
                                                int reservedQuantity, int unavailableQuantity) {
        return new InventoryRecord(skuId, warehouseId, totalQuantity, reservedQuantity, unavailableQuantity);
    }

    public int getAvailableQuantity() {
        return totalQuantity - reservedQuantity - unavailableQuantity;
    }

    public String getSkuId() { return skuId; }
    public String getWarehouseId() { return warehouseId; }
    public int getTotalQuantity() { return totalQuantity; }
    public int getReservedQuantity() { return reservedQuantity; }
    public int getUnavailableQuantity() { return unavailableQuantity; }
}
