package com.nexus.fulfillment.application.usecase;

import com.nexus.common.core.FieldError;
import com.nexus.common.core.exception.ValidationException;
import com.nexus.fulfillment.application.port.out.InventoryLedgerRepositoryPort;
import com.nexus.fulfillment.application.port.out.InventoryRecordRepositoryPort;
import com.nexus.fulfillment.domain.model.InventoryLedgerEntry;
import com.nexus.fulfillment.domain.model.LedgerMovementType;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public class IntakeInventoryUseCase {

    private final InventoryRecordRepositoryPort inventoryRecordRepositoryPort;
    private final InventoryLedgerRepositoryPort inventoryLedgerRepositoryPort;
    private final InventoryLevelNotifier inventoryLevelNotifier;

    public IntakeInventoryUseCase(InventoryRecordRepositoryPort inventoryRecordRepositoryPort,
                                   InventoryLedgerRepositoryPort inventoryLedgerRepositoryPort,
                                   InventoryLevelNotifier inventoryLevelNotifier) {
        this.inventoryRecordRepositoryPort = inventoryRecordRepositoryPort;
        this.inventoryLedgerRepositoryPort = inventoryLedgerRepositoryPort;
        this.inventoryLevelNotifier = inventoryLevelNotifier;
    }

    @Transactional
    public void intake(String skuId, String warehouseId, int quantity) {
        if (quantity <= 0) {
            throw new ValidationException(List.of(new FieldError("quantity", "quantity must be greater than zero")));
        }
        inventoryRecordRepositoryPort.intake(skuId, warehouseId, quantity);
        inventoryLedgerRepositoryPort.record(
                InventoryLedgerEntry.record(LedgerMovementType.INTAKE, skuId, warehouseId, quantity, null));
        inventoryLevelNotifier.stockChanged(skuId, warehouseId);
    }
}
