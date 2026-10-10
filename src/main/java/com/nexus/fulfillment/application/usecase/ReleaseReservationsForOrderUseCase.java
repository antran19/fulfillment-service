package com.nexus.fulfillment.application.usecase;

import com.nexus.fulfillment.application.port.out.ReservationRepositoryPort;
import org.springframework.transaction.annotation.Transactional;

// Triggered by OrderCancelled -- whether the buyer cancelled before paying or
// commerce-service auto-cancelled for an unrelated reason, either way every held line item
// for that order is released back to available stock.
public class ReleaseReservationsForOrderUseCase {

    private final ReservationRepositoryPort reservationRepositoryPort;
    private final ReleaseReservationUseCase releaseReservationUseCase;

    public ReleaseReservationsForOrderUseCase(ReservationRepositoryPort reservationRepositoryPort,
                                                ReleaseReservationUseCase releaseReservationUseCase) {
        this.reservationRepositoryPort = reservationRepositoryPort;
        this.releaseReservationUseCase = releaseReservationUseCase;
    }

    @Transactional
    public void releaseForOrder(String orderId) {
        for (var reservation : reservationRepositoryPort.findByOrderId(orderId)) {
            releaseReservationUseCase.release(reservation.getId());
        }
    }
}
