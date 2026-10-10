package com.nexus.fulfillment.api.dto.response;

import java.time.Instant;

public record ReservationResponse(String id, String referenceId, String skuId, String warehouseId, int quantity,
                                   String status, Instant expiresAt, Instant createdAt) {
}
