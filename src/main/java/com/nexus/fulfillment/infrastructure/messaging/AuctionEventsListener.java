package com.nexus.fulfillment.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.common.events.AuctionCreatedEvent;
import com.nexus.fulfillment.application.usecase.HoldForAuctionUseCase;
import com.nexus.fulfillment.application.usecase.ReleaseAuctionHoldUseCase;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

// Holds the auctioned unit for the auction's whole life and gives it back when there will be no
// sale. The winner's payment is seen on commerce-events (CommerceEventsListener commits the hold).
@Component
public class AuctionEventsListener {

    private final ObjectMapper objectMapper;
    private final HoldForAuctionUseCase holdForAuctionUseCase;
    private final ReleaseAuctionHoldUseCase releaseAuctionHoldUseCase;

    public AuctionEventsListener(ObjectMapper objectMapper, HoldForAuctionUseCase holdForAuctionUseCase,
                                 ReleaseAuctionHoldUseCase releaseAuctionHoldUseCase) {
        this.objectMapper = objectMapper;
        this.holdForAuctionUseCase = holdForAuctionUseCase;
        this.releaseAuctionHoldUseCase = releaseAuctionHoldUseCase;
    }

    // auto.offset.reset=latest, unlike this service's other listeners: the first time this group
    // reads auction-events it must NOT replay the whole history, or it would try to hold stock
    // for every auction that ever ran. Exceptions are NOT caught: KafkaErrorHandlingConfig
    // retries them, then moves the record to the dead-letter topic.
    @KafkaListener(topics = "auction-events", properties = "auto.offset.reset=latest")
    public void onMessage(String rawPayload) throws JsonProcessingException {
        JsonNode node = objectMapper.readTree(rawPayload);
        String auctionId = node.path("auctionId").asText();
        switch (node.path("eventType").asText()) {
            case "AuctionCreated" -> {
                AuctionCreatedEvent event = objectMapper.readValue(rawPayload, AuctionCreatedEvent.class);
                holdForAuctionUseCase.hold(event.getAuctionId(), event.getProductId());
            }
            case "AuctionFailed", "AuctionCancelled", "AuctionPaymentTimeout" ->
                    releaseAuctionHoldUseCase.release(auctionId);
            default -> {
                // AuctionWon keeps the hold for the winner; bids and lifecycle events change nothing here.
            }
        }
    }
}
