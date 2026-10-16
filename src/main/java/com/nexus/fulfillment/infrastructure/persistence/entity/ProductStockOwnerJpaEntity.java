package com.nexus.fulfillment.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "product_stock_owners")
public class ProductStockOwnerJpaEntity {

    @Id
    @Column(name = "product_id")
    private String productId;

    @Column(name = "seller_id", nullable = false)
    private String sellerId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ProductStockOwnerJpaEntity() {
    }

    public String getProductId() { return productId; }
    public String getSellerId() { return sellerId; }
    public Instant getCreatedAt() { return createdAt; }
}
