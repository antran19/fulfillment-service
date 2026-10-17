package com.nexus.fulfillment.application.usecase;

import com.nexus.common.events.InventoryLevelChangedEvent;
import com.nexus.fulfillment.application.port.out.EventPublisherPort;
import com.nexus.fulfillment.application.port.out.InventoryRecordRepositoryPort;
import com.nexus.fulfillment.domain.model.InventoryRecord;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class InventoryLevelNotifierTest {

    private final InventoryRecordRepositoryPort inventoryRecordRepositoryPort = mock(InventoryRecordRepositoryPort.class);
    private final EventPublisherPort eventPublisherPort = mock(EventPublisherPort.class);
    private final InventoryLevelNotifier notifier = new InventoryLevelNotifier(inventoryRecordRepositoryPort, eventPublisherPort);

    @Test
    void stockChanged_publishesTheAvailableQuantityAfterTheChange() {
        // total 10, reserved 3, unavailable 1 -> 6 can still be sold
        when(inventoryRecordRepositoryPort.findBySkuAndWarehouse("product-1", "MAIN"))
                .thenReturn(Optional.of(InventoryRecord.reconstitute("product-1", "MAIN", 10, 3, 1)));

        notifier.stockChanged("product-1", "MAIN");

        verify(eventPublisherPort).publish(argThat(e -> e instanceof InventoryLevelChangedEvent evt
                && evt.getProductId().equals("product-1") && evt.getAvailableQuantity() == 6));
    }

    @Test
    void stockChanged_withoutARecord_publishesNothing() {
        when(inventoryRecordRepositoryPort.findBySkuAndWarehouse("product-1", "MAIN")).thenReturn(Optional.empty());

        notifier.stockChanged("product-1", "MAIN");

        verify(eventPublisherPort, never()).publish(any());
    }
}
