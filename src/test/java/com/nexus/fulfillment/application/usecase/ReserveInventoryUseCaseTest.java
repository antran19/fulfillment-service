package com.nexus.fulfillment.application.usecase;

import com.nexus.common.core.exception.ValidationException;
import com.nexus.fulfillment.application.exception.InsufficientInventoryException;
import com.nexus.fulfillment.application.port.out.InventoryLedgerRepositoryPort;
import com.nexus.fulfillment.application.port.out.InventoryRecordRepositoryPort;
import com.nexus.fulfillment.application.port.out.ReservationRepositoryPort;
import com.nexus.fulfillment.domain.model.Reservation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class ReserveInventoryUseCaseTest {

    private final InventoryLevelNotifier inventoryLevelNotifier = mock(InventoryLevelNotifier.class);

    private InventoryRecordRepositoryPort inventoryRecordRepositoryPort;
    private ReservationRepositoryPort reservationRepositoryPort;
    private InventoryLedgerRepositoryPort inventoryLedgerRepositoryPort;
    private ReserveInventoryUseCase useCase;

    @BeforeEach
    void setUp() {
        inventoryRecordRepositoryPort = mock(InventoryRecordRepositoryPort.class);
        reservationRepositoryPort = mock(ReservationRepositoryPort.class);
        inventoryLedgerRepositoryPort = mock(InventoryLedgerRepositoryPort.class);
        useCase = new ReserveInventoryUseCase(inventoryRecordRepositoryPort, reservationRepositoryPort,
                inventoryLedgerRepositoryPort, inventoryLevelNotifier);
        when(reservationRepositoryPort.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void reserve_succeedsWhenStockIsAvailable() {
        when(reservationRepositoryPort.findByReferenceId("order-1:sku-1")).thenReturn(Optional.empty());
        when(inventoryRecordRepositoryPort.tryReserve("sku-1", "MAIN", 2)).thenReturn(true);

        Reservation reservation = useCase.reserve("order-1:sku-1", "sku-1", "MAIN", 2);

        assertThat(reservation.getReferenceId()).isEqualTo("order-1:sku-1");
        verify(reservationRepositoryPort).save(any(Reservation.class));
        verify(inventoryLedgerRepositoryPort).record(argThat(e -> e.getQuantity() == 2));
        verify(inventoryLevelNotifier).stockChanged("sku-1", "MAIN");
    }

    @Test
    void reserve_throwsWhenStockIsInsufficient() {
        when(reservationRepositoryPort.findByReferenceId("order-1:sku-1")).thenReturn(Optional.empty());
        when(inventoryRecordRepositoryPort.tryReserve("sku-1", "MAIN", 2)).thenReturn(false);

        assertThatThrownBy(() -> useCase.reserve("order-1:sku-1", "sku-1", "MAIN", 2))
                .isInstanceOf(InsufficientInventoryException.class);

        verify(reservationRepositoryPort, never()).save(any());
        verify(inventoryLedgerRepositoryPort, never()).record(any());
        verify(inventoryLevelNotifier, never()).stockChanged(any(), any());
    }

    @Test
    void reserve_isIdempotentForARepeatedReferenceId() {
        Reservation existing = Reservation.create("order-1:sku-1", "sku-1", "MAIN", 2);
        when(reservationRepositoryPort.findByReferenceId("order-1:sku-1")).thenReturn(Optional.of(existing));

        Reservation result = useCase.reserve("order-1:sku-1", "sku-1", "MAIN", 2);

        assertThat(result).isEqualTo(existing);
        verify(inventoryRecordRepositoryPort, never()).tryReserve(any(), any(), anyInt());
        verify(inventoryLevelNotifier, never()).stockChanged(any(), any());
        verify(reservationRepositoryPort, never()).save(any());
    }

    @Test
    void reserve_rejectsZeroOrNegativeQuantity() {
        assertThatThrownBy(() -> useCase.reserve("order-1:sku-1", "sku-1", "MAIN", 0))
                .isInstanceOf(ValidationException.class);

        verify(reservationRepositoryPort, never()).findByReferenceId(any());
    }
}
