package com.nexus.fulfillment.application.usecase;

import com.nexus.fulfillment.application.exception.ReservationNotFoundException;
import com.nexus.fulfillment.application.port.out.InventoryLedgerRepositoryPort;
import com.nexus.fulfillment.application.port.out.InventoryRecordRepositoryPort;
import com.nexus.fulfillment.application.port.out.ReservationRepositoryPort;
import com.nexus.fulfillment.domain.model.Reservation;
import com.nexus.fulfillment.domain.model.ReservationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class ReleaseReservationUseCaseTest {

    private InventoryRecordRepositoryPort inventoryRecordRepositoryPort;
    private ReservationRepositoryPort reservationRepositoryPort;
    private InventoryLedgerRepositoryPort inventoryLedgerRepositoryPort;
    private ReleaseReservationUseCase useCase;

    @BeforeEach
    void setUp() {
        inventoryRecordRepositoryPort = mock(InventoryRecordRepositoryPort.class);
        reservationRepositoryPort = mock(ReservationRepositoryPort.class);
        inventoryLedgerRepositoryPort = mock(InventoryLedgerRepositoryPort.class);
        useCase = new ReleaseReservationUseCase(inventoryRecordRepositoryPort, reservationRepositoryPort,
                inventoryLedgerRepositoryPort);
        when(reservationRepositoryPort.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void release_releasesStockAndMarksReservationReleased() {
        Reservation pending = Reservation.create("order-1:sku-1", "sku-1", "MAIN", 2);
        when(reservationRepositoryPort.findById(pending.getId())).thenReturn(Optional.of(pending));

        useCase.release(pending.getId());

        verify(inventoryRecordRepositoryPort).release("sku-1", "MAIN", 2);
        verify(reservationRepositoryPort).save(argThat(r -> r.getStatus() == ReservationStatus.RELEASED));
        verify(inventoryLedgerRepositoryPort).record(any());
    }

    @Test
    void release_isANoOpForAnAlreadyReleasedReservation() {
        Reservation released = Reservation.create("order-1:sku-1", "sku-1", "MAIN", 2).release();
        when(reservationRepositoryPort.findById(released.getId())).thenReturn(Optional.of(released));

        useCase.release(released.getId());

        verify(inventoryRecordRepositoryPort, never()).release(any(), any(), anyInt());
        verify(reservationRepositoryPort, never()).save(any());
    }

    @Test
    void release_throwsForAnUnknownReservation() {
        when(reservationRepositoryPort.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.release("missing")).isInstanceOf(ReservationNotFoundException.class);
    }
}
