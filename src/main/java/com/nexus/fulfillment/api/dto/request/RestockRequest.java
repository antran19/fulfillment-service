package com.nexus.fulfillment.api.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record RestockRequest(@Min(1) @Max(1_000_000) int quantity) {
}
