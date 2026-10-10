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

// Shared by the explicit "cancel my checkout" path and ExpireReservationsJob -- both just
// mean "this hold no longer applies," so both call the exact same use case.
public class ReleaseReservationUseCase {

    private final InventoryRecordRepositoryPort inventoryRecordRepositoryPort;
    private final ReservationRepositoryPort reservationRepositoryPort;
    private final InventoryLedgerRepositoryPort inventoryLedgerRepositoryPort;

    public ReleaseReservationUseCase(InventoryRecordRepositoryPort inventoryRecordRepositoryPort,
                                      ReservationRepositoryPort reservationRepositoryPort,
                                      InventoryLedgerRepositoryPort inventoryLedgerRepositoryPort) {
        this.inventoryRecordRepositoryPort = inventoryRecordRepositoryPort;
        this.reservationRepositoryPort = reservationRepositoryPort;
        this.inventoryLedgerRepositoryPort = inventoryLedgerRepositoryPort;
    }

    @Transactional
    public void release(String reservationId) {
        Reservation reservation = reservationRepositoryPort.findById(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException(reservationId));

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            return;
        }

        inventoryRecordRepositoryPort.release(reservation.getSkuId(), reservation.getWarehouseId(),
                reservation.getQuantity());
        reservationRepositoryPort.save(reservation.release());
        inventoryLedgerRepositoryPort.record(InventoryLedgerEntry.record(LedgerMovementType.RELEASE,
                reservation.getSkuId(), reservation.getWarehouseId(), reservation.getQuantity(),
                reservation.getReferenceId()));
    }
}
