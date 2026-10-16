package com.nexus.fulfillment.api.dto.response;

import java.time.Instant;

public record ReservationResponse(String id, String referenceId, String productId, String warehouseId, int quantity,
                                   String status, Instant expiresAt, Instant createdAt) {
}
