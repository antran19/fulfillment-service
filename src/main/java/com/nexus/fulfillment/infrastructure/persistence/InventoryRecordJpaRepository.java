package com.nexus.fulfillment.infrastructure.persistence;

import com.nexus.fulfillment.infrastructure.persistence.entity.InventoryRecordJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface InventoryRecordJpaRepository extends JpaRepository<InventoryRecordJpaEntity, UUID> {

    Optional<InventoryRecordJpaEntity> findBySkuIdAndWarehouseId(String skuId, String warehouseId);

    // Atomic upsert: ON CONFLICT means a brand-new SKU being intake'd concurrently from two
    // threads can never create two rows or lose an update -- Postgres serializes the conflict
    // resolution itself.
    @Modifying
    @Query(value = """
            INSERT INTO inventory_records (id, sku_id, warehouse_id, total_quantity, reserved_quantity, unavailable_quantity)
            VALUES (:id, :skuId, :warehouseId, :quantity, 0, 0)
            ON CONFLICT (sku_id, warehouse_id) DO UPDATE SET total_quantity = inventory_records.total_quantity + :quantity
            """, nativeQuery = true)
    void intake(@Param("id") UUID id, @Param("skuId") String skuId, @Param("warehouseId") String warehouseId,
                @Param("quantity") int quantity);

    // Returns the number of rows updated (0 or 1) -- the use case treats 0 as "insufficient
    // stock". The available-quantity check and the increment happen in the same statement,
    // so there is no window between "check" and "act" for a concurrent reservation to land in.
    @Modifying
    @Query("""
            UPDATE InventoryRecordJpaEntity r SET r.reservedQuantity = r.reservedQuantity + :quantity
            WHERE r.skuId = :skuId AND r.warehouseId = :warehouseId
              AND (r.totalQuantity - r.reservedQuantity - r.unavailableQuantity) >= :quantity
            """)
    int tryReserve(@Param("skuId") String skuId, @Param("warehouseId") String warehouseId,
                    @Param("quantity") int quantity);

    @Modifying
    @Query("""
            UPDATE InventoryRecordJpaEntity r SET r.reservedQuantity = r.reservedQuantity - :quantity
            WHERE r.skuId = :skuId AND r.warehouseId = :warehouseId
            """)
    int release(@Param("skuId") String skuId, @Param("warehouseId") String warehouseId,
                @Param("quantity") int quantity);

    @Modifying
    @Query("""
            UPDATE InventoryRecordJpaEntity r
            SET r.totalQuantity = r.totalQuantity - :quantity, r.reservedQuantity = r.reservedQuantity - :quantity
            WHERE r.skuId = :skuId AND r.warehouseId = :warehouseId
            """)
    int commit(@Param("skuId") String skuId, @Param("warehouseId") String warehouseId,
               @Param("quantity") int quantity);
}
