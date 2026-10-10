package com.nexus.fulfillment.application.port.out;

import com.nexus.fulfillment.domain.model.InventoryLedgerEntry;

public interface InventoryLedgerRepositoryPort {
    void record(InventoryLedgerEntry entry);
}
