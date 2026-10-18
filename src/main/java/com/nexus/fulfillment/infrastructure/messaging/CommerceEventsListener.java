package com.nexus.fulfillment.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.common.events.InventoryReservationRequestedEvent;
import com.nexus.common.events.OrderCancelledEvent;
import com.nexus.common.events.OrderPaidEvent;
import com.nexus.common.events.OrderReturnReceivedEvent;
import com.nexus.fulfillment.application.exception.InsufficientInventoryException;
import com.nexus.fulfillment.application.usecase.CommitAuctionHoldUseCase;
import com.nexus.fulfillment.application.usecase.CommitReservationsForOrderUseCase;
import com.nexus.fulfillment.application.usecase.FailOrderReservationUseCase;
import com.nexus.fulfillment.application.usecase.IntakeForReturnUseCase;
import com.nexus.fulfillment.application.usecase.ReleaseAuctionHoldUseCase;
import com.nexus.fulfillment.application.usecase.ReleaseReservationsForOrderUseCase;
import com.nexus.fulfillment.application.usecase.ReserveForOrderUseCase;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

// This is what wires the standalone Inventory core (see HANDOFF.md) into the rest of the
// system, asynchronously: commerce-service never waits for a synchronous answer, it finds
// out later whether stock held via InventoryReserved/InventoryReservationFailed on
// fulfillment-events (see CommerceEventsListener in commerce-service for the other side).
@Component
public class CommerceEventsListener {


    private final ObjectMapper objectMapper;
    private final ReserveForOrderUseCase reserveForOrderUseCase;
    private final FailOrderReservationUseCase failOrderReservationUseCase;
    private final CommitReservationsForOrderUseCase commitReservationsForOrderUseCase;
    private final ReleaseReservationsForOrderUseCase releaseReservationsForOrderUseCase;
    private final IntakeForReturnUseCase intakeForReturnUseCase;
    private final CommitAuctionHoldUseCase commitAuctionHoldUseCase;
    private final ReleaseAuctionHoldUseCase releaseAuctionHoldUseCase;

    public CommerceEventsListener(ObjectMapper objectMapper, ReserveForOrderUseCase reserveForOrderUseCase,
                                   FailOrderReservationUseCase failOrderReservationUseCase,
                                   CommitReservationsForOrderUseCase commitReservationsForOrderUseCase,
                                   ReleaseReservationsForOrderUseCase releaseReservationsForOrderUseCase,
                                   IntakeForReturnUseCase intakeForReturnUseCase,
                                   CommitAuctionHoldUseCase commitAuctionHoldUseCase,
                                   ReleaseAuctionHoldUseCase releaseAuctionHoldUseCase) {
        this.objectMapper = objectMapper;
        this.reserveForOrderUseCase = reserveForOrderUseCase;
        this.failOrderReservationUseCase = failOrderReservationUseCase;
        this.commitReservationsForOrderUseCase = commitReservationsForOrderUseCase;
        this.releaseReservationsForOrderUseCase = releaseReservationsForOrderUseCase;
        this.intakeForReturnUseCase = intakeForReturnUseCase;
        this.commitAuctionHoldUseCase = commitAuctionHoldUseCase;
        this.releaseAuctionHoldUseCase = releaseAuctionHoldUseCase;
    }

    // Exceptions are NOT caught here on purpose: KafkaErrorHandlingConfig retries them, then
    // moves the record to the dead-letter topic instead of losing it.
    @KafkaListener(topics = "commerce-events")
    public void onMessage(String rawPayload) throws Exception {
        String eventType = objectMapper.readTree(rawPayload).path("eventType").asText();
        switch (eventType) {
            case "InventoryReservationRequested" -> onReservationRequested(rawPayload);
            case "OrderPaid" -> {
                OrderPaidEvent event = objectMapper.readValue(rawPayload, OrderPaidEvent.class);
                commitReservationsForOrderUseCase.commitForOrder(event.getOrderId());
                // An auction order has no line-item hold of its own: its unit has been held for
                // the auction since it was created (see HoldForAuctionUseCase).
                if (event.getAuctionId() != null) {
                    commitAuctionHoldUseCase.commit(event.getAuctionId());
                }
            }
            case "OrderCancelled" -> {
                OrderCancelledEvent event = objectMapper.readValue(rawPayload, OrderCancelledEvent.class);
                releaseReservationsForOrderUseCase.releaseForOrder(event.getOrderId());
                if (event.getAuctionId() != null) {
                    releaseAuctionHoldUseCase.release(event.getAuctionId());
                }
            }
            // Restocking an order's items -- whether commerce-service fires this right away
            // (refund on an order that never shipped) or later once an Admin confirms a
            // shipped order's goods actually came back (ReturnRequest -> receive) -- is the
            // same operation from here: intake, not release. A PAID order's reservation was
            // already COMMITTED (stock permanently decremented), so there's no PENDING hold
            // left to release; see RefundOrderUseCase/ReceiveReturnUseCase in commerce-service.
            case "OrderReturnReceived" -> {
                OrderReturnReceivedEvent event = objectMapper.readValue(rawPayload, OrderReturnReceivedEvent.class);
                intakeForReturnUseCase.intakeForReturn(event.getItems());
            }
            default -> { /* irrelevant to this service */ }
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
