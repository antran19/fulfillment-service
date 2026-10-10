package com.nexus.fulfillment.application.usecase;

import com.nexus.common.core.FieldError;
import com.nexus.common.core.exception.ValidationException;
import com.nexus.fulfillment.application.exception.InsufficientInventoryException;
import com.nexus.fulfillment.application.port.out.InventoryLedgerRepositoryPort;
import com.nexus.fulfillment.application.port.out.InventoryRecordRepositoryPort;
import com.nexus.fulfillment.application.port.out.ReservationRepositoryPort;
import com.nexus.fulfillment.domain.model.InventoryLedgerEntry;
import com.nexus.fulfillment.domain.model.LedgerMovementType;
import com.nexus.fulfillment.domain.model.Reservation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public class ReserveInventoryUseCase {

    private final InventoryRecordRepositoryPort inventoryRecordRepositoryPort;
    private final ReservationRepositoryPort reservationRepositoryPort;
    private final InventoryLedgerRepositoryPort inventoryLedgerRepositoryPort;

    public ReserveInventoryUseCase(InventoryRecordRepositoryPort inventoryRecordRepositoryPort,
                                    ReservationRepositoryPort reservationRepositoryPort,
                                    InventoryLedgerRepositoryPort inventoryLedgerRepositoryPort) {
        this.inventoryRecordRepositoryPort = inventoryRecordRepositoryPort;
        this.reservationRepositoryPort = reservationRepositoryPort;
        this.inventoryLedgerRepositoryPort = inventoryLedgerRepositoryPort;
    }

    // Idempotent for a repeated referenceId under SEQUENTIAL retries (the realistic case: a
    // caller retries after a timeout, once the first attempt has already committed or failed).
    // Known limitation: two truly simultaneous calls with the same referenceId could both
    // pass the findByReferenceId check before either commits, and both reserve stock -- this
    // is not the scenario this service guards against (see tryReserve below for that one).
    // Guarding against it too would need a two-phase-transaction design; not worth it unless
    // real usage shows concurrent duplicate deliveries, not just sequential retries.
    @Transactional
    public Reservation reserve(String referenceId, String skuId, String warehouseId, int quantity) {
        if (quantity <= 0) {
            throw new ValidationException(List.of(new FieldError("quantity", "quantity must be greater than zero")));
        }

        Reservation existing = reservationRepositoryPort.findByReferenceId(referenceId).orElse(null);
        if (existing != null) {
            return existing;
        }

        // Atomic conditional UPDATE at the database level -- this, not application code, is
        // what makes concurrent reservations for the SAME sku/warehouse from DIFFERENT
        // referenceIds correct under real concurrency (two buyers, one unit left).
        boolean reserved = inventoryRecordRepositoryPort.tryReserve(skuId, warehouseId, quantity);
        if (!reserved) {
            throw new InsufficientInventoryException(skuId, warehouseId, quantity);
        }

        Reservation reservation = reservationRepositoryPort.save(Reservation.create(referenceId, skuId, warehouseId, quantity));
        inventoryLedgerRepositoryPort.record(
                InventoryLedgerEntry.record(LedgerMovementType.RESERVE, skuId, warehouseId, quantity, referenceId));
        return reservation;
    }
}
