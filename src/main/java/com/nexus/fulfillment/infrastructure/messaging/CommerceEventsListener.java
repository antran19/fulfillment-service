package com.nexus.fulfillment.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.common.events.InventoryReservationRequestedEvent;
import com.nexus.common.events.OrderCancelledEvent;
import com.nexus.common.events.OrderPaidEvent;
import com.nexus.fulfillment.application.exception.InsufficientInventoryException;
import com.nexus.fulfillment.application.usecase.CommitReservationsForOrderUseCase;
import com.nexus.fulfillment.application.usecase.FailOrderReservationUseCase;
import com.nexus.fulfillment.application.usecase.ReleaseReservationsForOrderUseCase;
import com.nexus.fulfillment.application.usecase.ReserveForOrderUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

// This is what wires the standalone Inventory core (see HANDOFF.md) into the rest of the
// system, asynchronously: commerce-service never waits for a synchronous answer, it finds
// out later whether stock held via InventoryReserved/InventoryReservationFailed on
// fulfillment-events (see CommerceEventsListener in commerce-service for the other side).
@Component
public class CommerceEventsListener {

    private static final Logger log = LoggerFactory.getLogger(CommerceEventsListener.class);

    private final ObjectMapper objectMapper;
    private final ReserveForOrderUseCase reserveForOrderUseCase;
    private final FailOrderReservationUseCase failOrderReservationUseCase;
    private final CommitReservationsForOrderUseCase commitReservationsForOrderUseCase;
    private final ReleaseReservationsForOrderUseCase releaseReservationsForOrderUseCase;

    public CommerceEventsListener(ObjectMapper objectMapper, ReserveForOrderUseCase reserveForOrderUseCase,
                                   FailOrderReservationUseCase failOrderReservationUseCase,
                                   CommitReservationsForOrderUseCase commitReservationsForOrderUseCase,
                                   ReleaseReservationsForOrderUseCase releaseReservationsForOrderUseCase) {
        this.objectMapper = objectMapper;
        this.reserveForOrderUseCase = reserveForOrderUseCase;
        this.failOrderReservationUseCase = failOrderReservationUseCase;
        this.commitReservationsForOrderUseCase = commitReservationsForOrderUseCase;
        this.releaseReservationsForOrderUseCase = releaseReservationsForOrderUseCase;
    }

    @KafkaListener(topics = "commerce-events")
    public void onMessage(String rawPayload) {
        try {
            String eventType = objectMapper.readTree(rawPayload).path("eventType").asText();
            switch (eventType) {
                case "InventoryReservationRequested" -> onReservationRequested(rawPayload);
                case "OrderPaid" -> {
                    OrderPaidEvent event = objectMapper.readValue(rawPayload, OrderPaidEvent.class);
                    commitReservationsForOrderUseCase.commitForOrder(event.getOrderId());
                }
                case "OrderCancelled" -> {
                    OrderCancelledEvent event = objectMapper.readValue(rawPayload, OrderCancelledEvent.class);
                    releaseReservationsForOrderUseCase.releaseForOrder(event.getOrderId());
                }
                default -> { /* irrelevant to this service */ }
            }
        } catch (Exception e) {
            log.error("Failed to process commerce-events message: {}", rawPayload, e);
        }
    }

    private void onReservationRequested(String rawPayload) throws Exception {
        InventoryReservationRequestedEvent event =
                objectMapper.readValue(rawPayload, InventoryReservationRequestedEvent.class);
        try {
            reserveForOrderUseCase.reserveForOrder(event.getOrderId(), event.getItems());
        } catch (InsufficientInventoryException e) {
            failOrderReservationUseCase.fail(event.getOrderId(), e.getMessage());
        }
    }
}
