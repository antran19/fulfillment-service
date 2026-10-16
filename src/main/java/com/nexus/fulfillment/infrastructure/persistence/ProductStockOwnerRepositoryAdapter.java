package com.nexus.fulfillment.infrastructure.persistence;

import com.nexus.fulfillment.application.port.out.ProductStockOwnerRepositoryPort;
import com.nexus.fulfillment.infrastructure.persistence.entity.ProductStockOwnerJpaEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ProductStockOwnerRepositoryAdapter implements ProductStockOwnerRepositoryPort {

    private final ProductStockOwnerJpaRepository jpaRepository;

    public ProductStockOwnerRepositoryAdapter(ProductStockOwnerJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean registerIfAbsent(String productId, String sellerId) {
        return jpaRepository.insertIfAbsent(productId, sellerId) == 1;
    }

    @Override
    public Optional<String> findSellerId(String productId) {
        return jpaRepository.findById(productId).map(ProductStockOwnerJpaEntity::getSellerId);
    }
}
