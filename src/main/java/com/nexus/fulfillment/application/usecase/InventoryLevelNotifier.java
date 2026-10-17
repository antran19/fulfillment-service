package com.nexus.fulfillment.application.usecase;

import com.nexus.common.events.InventoryLevelChangedEvent;
import com.nexus.fulfillment.application.port.out.EventPublisherPort;
import com.nexus.fulfillment.application.port.out.InventoryRecordRepositoryPort;

// Tells other services how much of a product can still be sold, right after the four use cases
// that change stock (intake, reserve, release, commit) -- every other stock movement goes
// through one of them. Runs inside the caller's transaction: the outbox row commits or rolls
// back with the stock change itself (a failed multi-item reservation publishes nothing).
// The event carries no warehouse: there is only the one default warehouse for now.
public class InventoryLevelNotifier {

    private final InventoryRecordRepositoryPort inventoryRecordRepositoryPort;
    private final EventPublisherPort eventPublisherPort;

    public InventoryLevelNotifier(InventoryRecordRepositoryPort inventoryRecordRepositoryPort,
                                  EventPublisherPort eventPublisherPort) {
        this.inventoryRecordRepositoryPort = inventoryRecordRepositoryPort;
        this.eventPublisherPort = eventPublisherPort;
    }

    public void stockChanged(String skuId, String warehouseId) {
        inventoryRecordRepositoryPort.findBySkuAndWarehouse(skuId, warehouseId).ifPresent(record ->
                eventPublisherPort.publish(new InventoryLevelChangedEvent(skuId, record.getAvailableQuantity())));
    }
}
