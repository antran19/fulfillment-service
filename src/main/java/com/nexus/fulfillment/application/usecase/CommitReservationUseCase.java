package com.nexus.fulfillment.application.usecase;

import com.nexus.fulfillment.application.exception.ReservationNotFoundException;
import com.nexus.fulfillment.application.port.out.InventoryLedgerRepositoryPort;
import com.nexus.fulfillment.application.port.out.InventoryRecordRepositoryPort;
import com.nexus.fulfillment.application.port.out.ReservationRepositoryPort;
import com.nexus.fulfillment.domain.model.InventoryLedgerEntry;
import com.nexus.fulfillment.domain.model.LedgerMovementType;
import com.nexus.fulfillment.domain.model.Reservation;
import com.nexus.fulfillment.domain.model.ReservationStatus;
import org.springframework.transaction.annotation.Transactional;

public class CommitReservationUseCase {

    private final InventoryRecordRepositoryPort inventoryRecordRepositoryPort;
    private final ReservationRepositoryPort reservationRepositoryPort;
    private final InventoryLedgerRepositoryPort inventoryLedgerRepositoryPort;

    public CommitReservationUseCase(InventoryRecordRepositoryPort inventoryRecordRepositoryPort,
                                     ReservationRepositoryPort reservationRepositoryPort,
                                     InventoryLedgerRepositoryPort inventoryLedgerRepositoryPort) {
        this.inventoryRecordRepositoryPort = inventoryRecordRepositoryPort;
        this.reservationRepositoryPort = reservationRepositoryPort;
        this.inventoryLedgerRepositoryPort = inventoryLedgerRepositoryPort;
    }

    // Idempotent: committing an already-committed (or released) reservation is a no-op --
    // same convention as PasswordResetToken/Order elsewhere in this codebase.
    @Transactional
    public void commit(String reservationId) {
        Reservation reservation = reservationRepositoryPort.findById(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException(reservationId));

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            return;
        }

        inventoryRecordRepositoryPort.commit(reservation.getSkuId(), reservation.getWarehouseId(),
                reservation.getQuantity());
        reservationRepositoryPort.save(reservation.commit());
        inventoryLedgerRepositoryPort.record(InventoryLedgerEntry.record(LedgerMovementType.COMMIT,
                reservation.getSkuId(), reservation.getWarehouseId(), reservation.getQuantity(),
                reservation.getReferenceId()));
    }
}
