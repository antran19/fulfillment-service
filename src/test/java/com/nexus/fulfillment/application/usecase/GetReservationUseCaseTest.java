package com.nexus.fulfillment.application.usecase;

import com.nexus.fulfillment.application.exception.ReservationNotFoundException;
import com.nexus.fulfillment.application.port.out.ReservationRepositoryPort;
import com.nexus.fulfillment.domain.model.Reservation;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetReservationUseCaseTest {

    private final ReservationRepositoryPort reservationRepositoryPort = mock(ReservationRepositoryPort.class);
    private final GetReservationUseCase useCase = new GetReservationUseCase(reservationRepositoryPort);

    @Test
    void get_returnsTheReservation() {
        Reservation reservation = Reservation.create("order-1:sku-1", "sku-1", "MAIN", 2);
        when(reservationRepositoryPort.findById(reservation.getId())).thenReturn(Optional.of(reservation));

        assertThat(useCase.get(reservation.getId())).isEqualTo(reservation);
    }

    @Test
    void get_throwsWhenNotFound() {
        when(reservationRepositoryPort.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.get("missing")).isInstanceOf(ReservationNotFoundException.class);
    }
}
