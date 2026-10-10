package com.nexus.fulfillment.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ReserveRequest(@NotBlank String referenceId, @NotBlank String skuId, String warehouseId,
                              @Min(1) int quantity) {
}
