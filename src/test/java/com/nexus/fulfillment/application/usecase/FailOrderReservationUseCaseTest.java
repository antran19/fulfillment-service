package com.nexus.fulfillment.application.usecase;

import com.nexus.common.events.InventoryReservationFailedEvent;
import com.nexus.fulfillment.application.port.out.EventPublisherPort;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class FailOrderReservationUseCaseTest {

    private final EventPublisherPort eventPublisherPort = mock(EventPublisherPort.class);
    private final FailOrderReservationUseCase useCase = new FailOrderReservationUseCase(eventPublisherPort);

    @Test
    void fail_publishesTheFailureEventWithTheReason() {
        useCase.fail("order-1", "Not enough stock for sku-2");

        verify(eventPublisherPort).publish(argThat(e ->
                e instanceof InventoryReservationFailedEvent evt && evt.getOrderId().equals("order-1")
                        && evt.getReason().equals("Not enough stock for sku-2")));
    }
}
