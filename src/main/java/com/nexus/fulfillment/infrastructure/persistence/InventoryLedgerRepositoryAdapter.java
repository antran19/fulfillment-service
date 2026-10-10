package com.nexus.fulfillment.infrastructure.persistence;

import com.nexus.fulfillment.application.port.out.InventoryLedgerRepositoryPort;
import com.nexus.fulfillment.domain.model.InventoryLedgerEntry;
import com.nexus.fulfillment.infrastructure.persistence.entity.InventoryLedgerJpaEntity;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class InventoryLedgerRepositoryAdapter implements InventoryLedgerRepositoryPort {

    private final InventoryLedgerJpaRepository jpaRepository;

    public InventoryLedgerRepositoryAdapter(InventoryLedgerJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void record(InventoryLedgerEntry entry) {
        jpaRepository.save(new InventoryLedgerJpaEntity(UUID.fromString(entry.getId()), entry.getSkuId(),
                entry.getWarehouseId(), entry.getMovementType().name(), entry.getQuantity(), entry.getReferenceId(),
                entry.getOccurredAt()));
    }
}
