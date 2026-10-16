package com.nexus.fulfillment.application.usecase;

import com.nexus.common.core.exception.ForbiddenException;
import com.nexus.fulfillment.application.port.out.ProductStockOwnerRepositoryPort;
import com.nexus.fulfillment.domain.model.Warehouse;
import org.springframework.transaction.annotation.Transactional;

// A Seller adds stock to a product of their own. canManageAny is true for Admin (holds
// INVENTORY.MANAGE), who may restock any product. A product with no recorded owner is treated
// as "not yours" rather than "not found": answering 404 would let a Seller probe which product
// ids exist, and an Admin can always restock it.
public class RestockInventoryUseCase {

    private final ProductStockOwnerRepositoryPort productStockOwnerRepositoryPort;
    private final IntakeInventoryUseCase intakeInventoryUseCase;

    public RestockInventoryUseCase(ProductStockOwnerRepositoryPort productStockOwnerRepositoryPort,
                                    IntakeInventoryUseCase intakeInventoryUseCase) {
        this.productStockOwnerRepositoryPort = productStockOwnerRepositoryPort;
        this.intakeInventoryUseCase = intakeInventoryUseCase;
    }

    @Transactional
    public void restock(String productId, int quantity, String callerId, boolean canManageAny) {
        if (!canManageAny) {
            boolean isOwner = productStockOwnerRepositoryPort.findSellerId(productId)
                    .map(owner -> owner.equals(callerId))
                    .orElse(false);
            if (!isOwner) {
                throw new ForbiddenException("NOT_THE_PRODUCT_OWNER",
                        "Only the seller who owns product " + productId + " can restock it");
            }
        }
        intakeInventoryUseCase.intake(productId, Warehouse.DEFAULT_ID, quantity);
    }
}
