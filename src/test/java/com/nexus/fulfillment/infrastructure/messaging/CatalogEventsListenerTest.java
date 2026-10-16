package com.nexus.fulfillment.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.common.events.ProductCreatedEvent;
import com.nexus.fulfillment.application.usecase.RegisterProductStockUseCase;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class CatalogEventsListenerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final RegisterProductStockUseCase registerProductStockUseCase = mock(RegisterProductStockUseCase.class);
    private final CatalogEventsListener listener = new CatalogEventsListener(objectMapper, registerProductStockUseCase);

    @Test
    void onMessage_productCreated_registersTheStockWithTheSellerAndInitialStock() throws Exception {
        // Serialize a real event so the test fails if the wire format ever drifts.
        String payload = objectMapper.writeValueAsString(
                new ProductCreatedEvent("p-1", "Watch", "cat-1", "seller-1", 12));

        listener.onMessage(payload);

        verify(registerProductStockUseCase).register("p-1", "seller-1", 12);
    }

    @Test
    void onMessage_anOldProductCreatedWithoutSellerOrStockStillParses() {
        String payload = "{\"eventId\":\"e-1\",\"eventType\":\"ProductCreated\",\"occurredAt\":\"2026-01-01T00:00:00Z\","
                + "\"aggregateId\":\"p-1\",\"productId\":\"p-1\",\"name\":\"Watch\",\"categoryId\":\"cat-1\"}";

        listener.onMessage(payload);

        verify(registerProductStockUseCase).register("p-1", null, 0);
    }

    @Test
    void onMessage_ignoresOtherCatalogEvents() {
        String payload = "{\"eventId\":\"e-2\",\"eventType\":\"ProductStatusChanged\","
                + "\"occurredAt\":\"2026-01-01T00:00:00Z\",\"aggregateId\":\"p-1\"}";

        listener.onMessage(payload);

        verify(registerProductStockUseCase, never()).register(anyString(), any(), anyInt());
    }

    @Test
    void onMessage_aFailingUseCaseOrBadPayloadDoesNotThrow() {
        doThrow(new IllegalStateException("db down")).when(registerProductStockUseCase).register(any(), any(), anyInt());
        String payload = "{\"eventId\":\"e-3\",\"eventType\":\"ProductCreated\",\"occurredAt\":\"2026-01-01T00:00:00Z\","
                + "\"aggregateId\":\"p-1\",\"productId\":\"p-1\",\"sellerId\":\"s-1\",\"initialStock\":1}";

        assertThatCode(() -> listener.onMessage(payload)).doesNotThrowAnyException();
        assertThatCode(() -> listener.onMessage("not json")).doesNotThrowAnyException();
    }
}
