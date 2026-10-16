package com.nexus.fulfillment.infrastructure.persistence;

import com.nexus.fulfillment.infrastructure.persistence.entity.ProductStockOwnerJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductStockOwnerJpaRepository extends JpaRepository<ProductStockOwnerJpaEntity, String> {

    // One atomic statement: two concurrent deliveries of the same event cannot both insert.
    // Returns 1 when the row was created, 0 when it already existed.
    @Modifying
    @Query(value = "INSERT INTO product_stock_owners (product_id, seller_id, created_at) "
            + "VALUES (:productId, :sellerId, now()) ON CONFLICT (product_id) DO NOTHING", nativeQuery = true)
    int insertIfAbsent(@Param("productId") String productId, @Param("sellerId") String sellerId);
}
