package com.nexus.fulfillment.application.usecase;

import com.nexus.fulfillment.application.port.out.InventoryLedgerRepositoryPort;
import com.nexus.fulfillment.application.port.out.InventoryRecordRepositoryPort;
import com.nexus.fulfillment.application.port.out.ReservationRepositoryPort;
import com.nexus.fulfillment.domain.model.Reservation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CommitReservationsForOrderUseCaseTest {

    private final InventoryLevelNotifier inventoryLevelNotifier = mock(InventoryLevelNotifier.class);

    private final InventoryRecordRepositoryPort inventoryRecordRepositoryPort = mock(InventoryRecordRepositoryPort.class);
    private final ReservationRepositoryPort reservationRepositoryPort = mock(ReservationRepositoryPort.class);
    private final InventoryLedgerRepositoryPort inventoryLedgerRepositoryPort = mock(InventoryLedgerRepositoryPort.class);
    private final CommitReservationUseCase commitReservationUseCase = new CommitReservationUseCase(
            inventoryRecordRepositoryPort, reservationRepositoryPort, inventoryLedgerRepositoryPort,
            inventoryLevelNotifier);
    private final CommitReservationsForOrderUseCase useCase =
            new CommitReservationsForOrderUseCase(reservationRepositoryPort, commitReservationUseCase);

    @Test
    void commitForOrder_commitsEveryLineItemReservation() {
        Reservation item1 = Reservation.create("order-1:sku-1", "sku-1", "MAIN", 2);
        Reservation item2 = Reservation.create("order-1:sku-2", "sku-2", "MAIN", 1);
        when(reservationRepositoryPort.findByOrderId("order-1")).thenReturn(List.of(item1, item2));
        when(reservationRepositoryPort.findById(item1.getId())).thenReturn(Optional.of(item1));
        when(reservationRepositoryPort.findById(item2.getId())).thenReturn(Optional.of(item2));
        when(reservationRepositoryPort.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.commitForOrder("order-1");

        verify(inventoryRecordRepositoryPort).commit("sku-1", "MAIN", 2);
        verify(inventoryRecordRepositoryPort).commit("sku-2", "MAIN", 1);
    }
}
