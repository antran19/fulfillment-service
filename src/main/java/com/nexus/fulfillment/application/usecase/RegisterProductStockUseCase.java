package com.nexus.fulfillment.application.usecase;

import com.nexus.fulfillment.application.port.out.ProductStockOwnerRepositoryPort;
import com.nexus.fulfillment.domain.model.Warehouse;
import org.springframework.transaction.annotation.Transactional;

// Reacts to catalog-service's ProductCreated event: records who owns the product's stock and
// opens its inventory with the seller's initial quantity. Everything here is keyed by the
// PRODUCT id, the same value commerce-service sends as "skuId" when it reserves stock, so the
// stock registered here is exactly what a buyer's order will find.
public class RegisterProductStockUseCase {

    private final ProductStockOwnerRepositoryPort productStockOwnerRepositoryPort;
    private final IntakeInventoryUseCase intakeInventoryUseCase;

    public RegisterProductStockUseCase(ProductStockOwnerRepositoryPort productStockOwnerRepositoryPort,
                                        IntakeInventoryUseCase intakeInventoryUseCase) {
        this.productStockOwnerRepositoryPort = productStockOwnerRepositoryPort;
        this.intakeInventoryUseCase = intakeInventoryUseCase;
    }

    // One transaction for owner + intake, so a redelivered event is a no-op: the owner row
    // already exists, so the initial stock is NOT added a second time.
    @Transactional
    public void register(String productId, String sellerId, int initialStock) {
        if (productId == null || sellerId == null || sellerId.isBlank()) {
            // A ProductCreated published before sellerId existed: the owner is unknown, so
            // nothing can be registered. An Admin can still intake stock for it.
            return;
        }
        boolean created = productStockOwnerRepositoryPort.registerIfAbsent(productId, sellerId);
        if (created && initialStock > 0) {
            intakeInventoryUseCase.intake(productId, Warehouse.DEFAULT_ID, initialStock);
        }
    }
}
