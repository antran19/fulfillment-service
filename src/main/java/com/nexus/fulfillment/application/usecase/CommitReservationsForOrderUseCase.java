package com.nexus.fulfillment.application.usecase;

import com.nexus.fulfillment.application.port.out.ReservationRepositoryPort;
import org.springframework.transaction.annotation.Transactional;

// Triggered by OrderPaid. commitReservationUseCase.commit() is already idempotent per
// reservation, so a redelivered OrderPaid is harmless.
public class CommitReservationsForOrderUseCase {

    private final ReservationRepositoryPort reservationRepositoryPort;
    private final CommitReservationUseCase commitReservationUseCase;

    public CommitReservationsForOrderUseCase(ReservationRepositoryPort reservationRepositoryPort,
                                               CommitReservationUseCase commitReservationUseCase) {
        this.reservationRepositoryPort = reservationRepositoryPort;
        this.commitReservationUseCase = commitReservationUseCase;
    }

    @Transactional
    public void commitForOrder(String orderId) {
        for (var reservation : reservationRepositoryPort.findByOrderId(orderId)) {
            commitReservationUseCase.commit(reservation.getId());
        }
    }
}
