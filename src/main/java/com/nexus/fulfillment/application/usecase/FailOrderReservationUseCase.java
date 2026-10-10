package com.nexus.fulfillment.application.usecase;

import com.nexus.common.events.InventoryReservationFailedEvent;
import com.nexus.fulfillment.application.port.out.EventPublisherPort;
import org.springframework.transaction.annotation.Transactional;

// Called by CommerceEventsListener's catch block after ReserveForOrderUseCase's transaction
// has already rolled back -- this runs in its own fresh transaction, just to queue the
// failure event.
public class FailOrderReservationUseCase {

    private final EventPublisherPort eventPublisherPort;

    public FailOrderReservationUseCase(EventPublisherPort eventPublisherPort) {
        this.eventPublisherPort = eventPublisherPort;
    }

    @Transactional
    public void fail(String orderId, String reason) {
        eventPublisherPort.publish(new InventoryReservationFailedEvent(orderId, reason));
    }
}
