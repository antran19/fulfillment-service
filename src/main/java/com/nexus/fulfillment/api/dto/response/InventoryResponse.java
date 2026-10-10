package com.nexus.fulfillment.api.dto.response;

public record InventoryResponse(String skuId, String warehouseId, int totalQuantity, int reservedQuantity,
                                 int unavailableQuantity, int availableQuantity) {
}
