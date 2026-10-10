package com.nexus.fulfillment.infrastructure.persistence;

import com.nexus.fulfillment.application.port.out.InventoryRecordRepositoryPort;
import com.nexus.fulfillment.domain.model.InventoryRecord;
import com.nexus.fulfillment.infrastructure.persistence.entity.InventoryRecordJpaEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
public class InventoryRecordRepositoryAdapter implements InventoryRecordRepositoryPort {

    private final InventoryRecordJpaRepository jpaRepository;

    public InventoryRecordRepositoryAdapter(InventoryRecordJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<InventoryRecord> findBySkuAndWarehouse(String skuId, String warehouseId) {
        return jpaRepository.findBySkuIdAndWarehouseId(skuId, warehouseId).map(this::toDomain);
    }

    @Transactional
    @Override
    public void intake(String skuId, String warehouseId, int quantity) {
        jpaRepository.intake(UUID.randomUUID(), skuId, warehouseId, quantity);
    }

    @Transactional
    @Override
    public boolean tryReserve(String skuId, String warehouseId, int quantity) {
        return jpaRepository.tryReserve(skuId, warehouseId, quantity) > 0;
    }

    @Transactional
    @Override
    public void release(String skuId, String warehouseId, int quantity) {
        jpaRepository.release(skuId, warehouseId, quantity);
    }

    @Transactional
    @Override
    public void commit(String skuId, String warehouseId, int quantity) {
        jpaRepository.commit(skuId, warehouseId, quantity);
    }

    private InventoryRecord toDomain(InventoryRecordJpaEntity entity) {
        return InventoryRecord.reconstitute(entity.getSkuId(), entity.getWarehouseId(), entity.getTotalQuantity(),
                entity.getReservedQuantity(), entity.getUnavailableQuantity());
    }
}
