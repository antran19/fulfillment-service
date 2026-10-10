package com.nexus.fulfillment.application.usecase;

import com.nexus.fulfillment.application.exception.ReservationNotFoundException;
import com.nexus.fulfillment.application.port.out.ReservationRepositoryPort;
import com.nexus.fulfillment.domain.model.Reservation;

public class GetReservationUseCase {

    private final ReservationRepositoryPort reservationRepositoryPort;

    public GetReservationUseCase(ReservationRepositoryPort reservationRepositoryPort) {
        this.reservationRepositoryPort = reservationRepositoryPort;
    }

    public Reservation get(String id) {
        return reservationRepositoryPort.findById(id).orElseThrow(() -> new ReservationNotFoundException(id));
    }
}
