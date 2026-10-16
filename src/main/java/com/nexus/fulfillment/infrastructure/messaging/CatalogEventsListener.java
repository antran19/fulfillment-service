package com.nexus.fulfillment.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.common.events.ProductCreatedEvent;
import com.nexus.fulfillment.application.usecase.RegisterProductStockUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

// Listens to catalog-service's events so a newly listed product gets its inventory record and
// a recorded owner. Same convention as CommerceEventsListener: a message that fails to process
// is logged and skipped, not retried -- an Admin can still intake stock for such a product.
@Component
public class CatalogEventsListener {

    private static final Logger log = LoggerFactory.getLogger(CatalogEventsListener.class);

    private final ObjectMapper objectMapper;
    private final RegisterProductStockUseCase registerProductStockUseCase;

    public CatalogEventsListener(ObjectMapper objectMapper, RegisterProductStockUseCase registerProductStockUseCase) {
        this.objectMapper = objectMapper;
        this.registerProductStockUseCase = registerProductStockUseCase;
    }

    @KafkaListener(topics = "catalog-events")
    public void onMessage(String rawPayload) {
        try {
            String eventType = objectMapper.readTree(rawPayload).path("eventType").asText();
            if ("ProductCreated".equals(eventType)) {
                ProductCreatedEvent event = objectMapper.readValue(rawPayload, ProductCreatedEvent.class);
                registerProductStockUseCase.register(event.getProductId(), event.getSellerId(),
                        event.getInitialStock());
            }
        } catch (Exception e) {
            log.error("Failed to process catalog-events message: {}", rawPayload, e);
        }
    }
}
