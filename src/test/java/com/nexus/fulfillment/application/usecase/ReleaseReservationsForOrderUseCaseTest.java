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

class ReleaseReservationsForOrderUseCaseTest {

    private final InventoryLevelNotifier inventoryLevelNotifier = mock(InventoryLevelNotifier.class);

    private final InventoryRecordRepositoryPort inventoryRecordRepositoryPort = mock(InventoryRecordRepositoryPort.class);
    private final ReservationRepositoryPort reservationRepositoryPort = mock(ReservationRepositoryPort.class);
    private final InventoryLedgerRepositoryPort inventoryLedgerRepositoryPort = mock(InventoryLedgerRepositoryPort.class);
    private final ReleaseReservationUseCase releaseReservationUseCase = new ReleaseReservationUseCase(
            inventoryRecordRepositoryPort, reservationRepositoryPort, inventoryLedgerRepositoryPort,
            inventoryLevelNotifier);
    private final ReleaseReservationsForOrderUseCase useCase =
            new ReleaseReservationsForOrderUseCase(reservationRepositoryPort, releaseReservationUseCase);

    @Test
    void releaseForOrder_releasesEveryLineItemReservation() {
        Reservation item1 = Reservation.create("order-1:sku-1", "sku-1", "MAIN", 2);
        when(reservationRepositoryPort.findByOrderId("order-1")).thenReturn(List.of(item1));
        when(reservationRepositoryPort.findById(item1.getId())).thenReturn(Optional.of(item1));
        when(reservationRepositoryPort.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.releaseForOrder("order-1");

        verify(inventoryRecordRepositoryPort).release("sku-1", "MAIN", 2);
    }
}
