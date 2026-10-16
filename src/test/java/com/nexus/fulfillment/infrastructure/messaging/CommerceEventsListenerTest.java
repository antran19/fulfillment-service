package com.nexus.fulfillment.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.fulfillment.application.exception.InsufficientInventoryException;
import com.nexus.fulfillment.application.usecase.CommitReservationsForOrderUseCase;
import com.nexus.fulfillment.application.usecase.FailOrderReservationUseCase;
import com.nexus.fulfillment.application.usecase.IntakeForReturnUseCase;
import com.nexus.fulfillment.application.usecase.ReleaseReservationsForOrderUseCase;
import com.nexus.fulfillment.application.usecase.ReserveForOrderUseCase;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CommerceEventsListenerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final ReserveForOrderUseCase reserveForOrderUseCase = mock(ReserveForOrderUseCase.class);
    private final FailOrderReservationUseCase failOrderReservationUseCase = mock(FailOrderReservationUseCase.class);
    private final CommitReservationsForOrderUseCase commitReservationsForOrderUseCase =
            mock(CommitReservationsForOrderUseCase.class);
    private final ReleaseReservationsForOrderUseCase releaseReservationsForOrderUseCase =
            mock(ReleaseReservationsForOrderUseCase.class);
    private final IntakeForReturnUseCase intakeForReturnUseCase = mock(IntakeForReturnUseCase.class);
    private final CommerceEventsListener listener = new CommerceEventsListener(objectMapper, reserveForOrderUseCase,
            failOrderReservationUseCase, commitReservationsForOrderUseCase, releaseReservationsForOrderUseCase,
            intakeForReturnUseCase);

    @Test
    void onMessage_reservationRequested_reservesForTheOrder() throws Exception {
        String payload = "{\"eventId\":\"e-1\",\"eventType\":\"InventoryReservationRequested\","
                + "\"occurredAt\":\"2026-01-01T00:00:00Z\",\"aggregateId\":\"order-1\",\"orderId\":\"order-1\","
                + "\"items\":[{\"skuId\":\"sku-1\",\"quantity\":2}]}";

        listener.onMessage(payload);

        verify(reserveForOrderUseCase).reserveForOrder(eq("order-1"), any());
        verify(failOrderReservationUseCase, never()).fail(any(), any());
    }

    @Test
    void onMessage_reservationRequested_whenInsufficientStock_callsFail() throws Exception {
        String payload = "{\"eventId\":\"e-2\",\"eventType\":\"InventoryReservationRequested\","
                + "\"occurredAt\":\"2026-01-01T00:00:00Z\",\"aggregateId\":\"order-2\",\"orderId\":\"order-2\","
                + "\"items\":[{\"skuId\":\"sku-1\",\"quantity\":99}]}";
        doThrow(new InsufficientInventoryException("sku-1", "MAIN", 99))
                .when(reserveForOrderUseCase).reserveForOrder(any(), any());

        assertThatCode(() -> listener.onMessage(payload)).doesNotThrowAnyException();

        verify(failOrderReservationUseCase).fail(eq("order-2"), any());
    }

    @Test
    void onMessage_orderPaid_commitsForTheOrder() throws Exception {
        String payload = "{\"eventId\":\"e-3\",\"eventType\":\"OrderPaid\",\"occurredAt\":\"2026-01-01T00:00:00Z\","
                + "\"aggregateId\":\"order-3\",\"orderId\":\"order-3\",\"buyerId\":\"buyer-1\",\"amount\":100.00}";

        listener.onMessage(payload);

        verify(commitReservationsForOrderUseCase).commitForOrder("order-3");
    }

    @Test
    void onMessage_orderCancelled_releasesForTheOrder() throws Exception {
        String payload = "{\"eventId\":\"e-4\",\"eventType\":\"OrderCancelled\",\"occurredAt\":\"2026-01-01T00:00:00Z\","
                + "\"aggregateId\":\"order-4\",\"orderId\":\"order-4\",\"reason\":\"BUYER_REQUESTED\"}";

        listener.onMessage(payload);

        verify(releaseReservationsForOrderUseCase).releaseForOrder("order-4");
    }

    @Test
    void onMessage_orderReturnReceived_intakesTheReturnedItems() throws Exception {
        String payload = "{\"eventId\":\"e-8\",\"eventType\":\"OrderReturnReceived\","
                + "\"occurredAt\":\"2026-01-01T00:00:00Z\",\"aggregateId\":\"order-8\",\"orderId\":\"order-8\","
                + "\"items\":[{\"skuId\":\"sku-1\",\"quantity\":2}]}";

        listener.onMessage(payload);

        verify(intakeForReturnUseCase).intakeForReturn(any());
    }

    @Test
    void onMessage_orderRefunded_isIgnored() throws Exception {
        // commerce-service's RefundOrderUseCase handles the restock decision itself (publishing
        // OrderReturnReceivedEvent directly when the order hadn't shipped) -- this event is purely
        // informational on this side, nothing here needs to react to it.
        String payload = "{\"eventId\":\"e-9\",\"eventType\":\"OrderRefunded\",\"occurredAt\":\"2026-01-01T00:00:00Z\","
                + "\"aggregateId\":\"order-9\",\"orderId\":\"order-9\",\"buyerId\":\"buyer-1\",\"amount\":100.00,"
                + "\"reason\":\"test\",\"shipped\":false}";

        listener.onMessage(payload);

        verifyNoInteractions(reserveForOrderUseCase, failOrderReservationUseCase, commitReservationsForOrderUseCase,
                releaseReservationsForOrderUseCase, intakeForReturnUseCase);
    }

    @Test
    void onMessage_unrelatedEventType_doesNothing() throws Exception {
        String payload = "{\"eventId\":\"e-5\",\"eventType\":\"SomethingElse\",\"occurredAt\":\"2026-01-01T00:00:00Z\","
                + "\"aggregateId\":\"x\"}";

        listener.onMessage(payload);

        verifyNoInteractions(reserveForOrderUseCase, failOrderReservationUseCase, commitReservationsForOrderUseCase,
                releaseReservationsForOrderUseCase, intakeForReturnUseCase);
    }

    @Test
    void onMessage_malformedPayload_throwsSoItGoesToDeadLetterTopic() throws Exception {
        assertThatThrownBy(() -> listener.onMessage("{not valid json"))
                .isInstanceOf(JsonProcessingException.class);
    }
}
