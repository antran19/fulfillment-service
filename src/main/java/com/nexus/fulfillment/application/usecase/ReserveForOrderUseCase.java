package com.nexus.fulfillment.application.usecase;

import com.nexus.common.events.InventoryReservedEvent;
import com.nexus.common.events.ReservationLineItem;
import com.nexus.fulfillment.application.port.out.EventPublisherPort;
import com.nexus.fulfillment.domain.model.Warehouse;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Orchestrates a whole order's line items as ONE transaction, deliberately not catching
// InsufficientInventoryException itself: if any line item fails, letting the exception
// propagate rolls back every reservation already made earlier in this same loop (Spring's
// transaction rollback, not manual compensation) -- so a partially-reserved order can never
// exist. The caller (CommerceEventsListener) catches the exception and publishes the
// failure event in its own separate transaction instead.
public class ReserveForOrderUseCase {

    private final ReserveInventoryUseCase reserveInventoryUseCase;
    private final EventPublisherPort eventPublisherPort;

    public ReserveForOrderUseCase(ReserveInventoryUseCase reserveInventoryUseCase,
                                   EventPublisherPort eventPublisherPort) {
        this.reserveInventoryUseCase = reserveInventoryUseCase;
        this.eventPublisherPort = eventPublisherPort;
    }

    @Transactional
    public void reserveForOrder(String orderId, List<ReservationLineItem> items) {
        for (ReservationLineItem item : items) {
            reserveInventoryUseCase.reserve(orderId + ":" + item.skuId(), item.skuId(), Warehouse.DEFAULT_ID,
                    item.quantity());
        }
        eventPublisherPort.publish(new InventoryReservedEvent(orderId));
    }
}
