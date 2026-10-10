package com.nexus.fulfillment.infrastructure.persistence;

import com.nexus.fulfillment.infrastructure.persistence.entity.InventoryLedgerJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InventoryLedgerJpaRepository extends JpaRepository<InventoryLedgerJpaEntity, UUID> {
}
