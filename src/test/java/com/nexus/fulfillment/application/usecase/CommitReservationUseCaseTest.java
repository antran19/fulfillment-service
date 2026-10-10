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

class CommitReservationUseCaseTest {

    private InventoryRecordRepositoryPort inventoryRecordRepositoryPort;
    private ReservationRepositoryPort reservationRepositoryPort;
    private InventoryLedgerRepositoryPort inventoryLedgerRepositoryPort;
    private CommitReservationUseCase useCase;

    @BeforeEach
    void setUp() {
        inventoryRecordRepositoryPort = mock(InventoryRecordRepositoryPort.class);
        reservationRepositoryPort = mock(ReservationRepositoryPort.class);
        inventoryLedgerRepositoryPort = mock(InventoryLedgerRepositoryPort.class);
        useCase = new CommitReservationUseCase(inventoryRecordRepositoryPort, reservationRepositoryPort,
                inventoryLedgerRepositoryPort);
        when(reservationRepositoryPort.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void commit_commitsStockAndMarksReservationCommitted() {
        Reservation pending = Reservation.create("order-1:sku-1", "sku-1", "MAIN", 2);
        when(reservationRepositoryPort.findById(pending.getId())).thenReturn(Optional.of(pending));

        useCase.commit(pending.getId());

        verify(inventoryRecordRepositoryPort).commit("sku-1", "MAIN", 2);
        verify(reservationRepositoryPort).save(argThat(r -> r.getStatus() == ReservationStatus.COMMITTED));
        verify(inventoryLedgerRepositoryPort).record(any());
    }

    @Test
    void commit_isANoOpForAnAlreadyCommittedReservation() {
        Reservation committed = Reservation.create("order-1:sku-1", "sku-1", "MAIN", 2).commit();
        when(reservationRepositoryPort.findById(committed.getId())).thenReturn(Optional.of(committed));

        useCase.commit(committed.getId());

        verify(inventoryRecordRepositoryPort, never()).commit(any(), any(), anyInt());
        verify(reservationRepositoryPort, never()).save(any());
    }

    @Test
    void commit_throwsForAnUnknownReservation() {
        when(reservationRepositoryPort.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.commit("missing")).isInstanceOf(ReservationNotFoundException.class);
    }
}
