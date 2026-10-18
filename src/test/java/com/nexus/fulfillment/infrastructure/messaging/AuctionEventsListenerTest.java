package com.nexus.fulfillment.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.fulfillment.application.usecase.HoldForAuctionUseCase;
import com.nexus.fulfillment.application.usecase.ReleaseAuctionHoldUseCase;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class AuctionEventsListenerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final HoldForAuctionUseCase holdForAuctionUseCase = mock(HoldForAuctionUseCase.class);
    private final ReleaseAuctionHoldUseCase releaseAuctionHoldUseCase = mock(ReleaseAuctionHoldUseCase.class);
    private final AuctionEventsListener listener =
            new AuctionEventsListener(objectMapper, holdForAuctionUseCase, releaseAuctionHoldUseCase);

    private static String event(String type, String extra) {
        return "{\"eventId\":\"e\",\"eventType\":\"" + type + "\",\"occurredAt\":\"2026-01-01T00:00:00Z\","
                + "\"aggregateId\":\"auction-1\",\"auctionId\":\"auction-1\"" + extra + "}";
    }

    @Test
    void auctionCreated_holdsAUnit() throws Exception {
        listener.onMessage(event("AuctionCreated", ",\"productId\":\"product-1\",\"sellerId\":\"seller-1\""));

        verify(holdForAuctionUseCase).hold("auction-1", "product-1");
    }

    @Test
    void auctionWithoutWinner_cancelled_orWinnerDefaulted_releasesTheUnit() throws Exception {
        listener.onMessage(event("AuctionFailed", ""));
        listener.onMessage(event("AuctionCancelled", ",\"cancelledBy\":\"seller-1\""));
        listener.onMessage(event("AuctionPaymentTimeout", ",\"winnerId\":\"w\",\"productId\":\"product-1\""));

        verify(releaseAuctionHoldUseCase, times(3)).release("auction-1");
    }

    @Test
    void auctionWon_keepsTheHold() throws Exception {
        listener.onMessage(event("AuctionWon", ",\"productId\":\"product-1\",\"sellerId\":\"s\",\"winnerId\":\"w\",\"finalPrice\":10"));

        verifyNoInteractions(holdForAuctionUseCase, releaseAuctionHoldUseCase);
    }
}
