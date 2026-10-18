package com.nexus.fulfillment.application.usecase;

import com.nexus.common.events.AuctionHoldFailedEvent;
import com.nexus.fulfillment.application.exception.InsufficientInventoryException;
import com.nexus.fulfillment.application.port.out.EventPublisherPort;
import com.nexus.fulfillment.domain.model.Warehouse;

import java.time.Duration;

// Holds the auctioned unit from the moment the auction is created, so it cannot be sold
// elsewhere while bidding runs and the winner is sure to get it. Released when the auction ends
// without a winner, is cancelled or the winner defaults; committed when the winner pays.
// Deliberately NOT @Transactional: the reservation runs (and on failure rolls back) in its own
// transaction, then the failure event is written in a fresh one; inside one shared transaction
// the failed reservation would mark it rollback-only and take the failure event down with it.
public class HoldForAuctionUseCase {

    // Longest auction (7 days) + the winner's 24h to pay, with room to spare. Only a safety net:
    // the auction's own end/cancel/payment events release or commit the hold.
    static final Duration AUCTION_HOLD = Duration.ofDays(10);

    private final ReserveInventoryUseCase reserveInventoryUseCase;
    private final EventPublisherPort eventPublisherPort;

    public HoldForAuctionUseCase(ReserveInventoryUseCase reserveInventoryUseCase, EventPublisherPort eventPublisherPort) {
        this.reserveInventoryUseCase = reserveInventoryUseCase;
        this.eventPublisherPort = eventPublisherPort;
    }

    public static String referenceFor(String auctionId) {
        return "auction:" + auctionId;
    }

    public void hold(String auctionId, String productId) {
        try {
            reserveInventoryUseCase.reserve(referenceFor(auctionId), productId, Warehouse.DEFAULT_ID, 1, AUCTION_HOLD);
        } catch (InsufficientInventoryException e) {
            // auction-service's copy of the stock lagged behind a sale: tell it to cancel.
            eventPublisherPort.publish(new AuctionHoldFailedEvent(auctionId, e.getMessage()));
        }
    }
}
