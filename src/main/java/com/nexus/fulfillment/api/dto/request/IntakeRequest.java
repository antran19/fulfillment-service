package com.nexus.fulfillment.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record IntakeRequest(@NotBlank String productId, String warehouseId, @Min(1) int quantity) {
}
