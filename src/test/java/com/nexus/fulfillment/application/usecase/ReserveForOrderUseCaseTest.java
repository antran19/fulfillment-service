package com.nexus.fulfillment.application.usecase;

import com.nexus.common.events.InventoryReservedEvent;
import com.nexus.common.events.ReservationLineItem;
import com.nexus.fulfillment.application.exception.InsufficientInventoryException;
import com.nexus.fulfillment.application.port.out.EventPublisherPort;
import com.nexus.fulfillment.application.port.out.InventoryLedgerRepositoryPort;
import com.nexus.fulfillment.application.port.out.InventoryRecordRepositoryPort;
import com.nexus.fulfillment.application.port.out.ReservationRepositoryPort;
import com.nexus.fulfillment.domain.model.Reservation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ReserveForOrderUseCaseTest {

    private InventoryRecordRepositoryPort inventoryRecordRepositoryPort;
    private ReservationRepositoryPort reservationRepositoryPort;
    private InventoryLedgerRepositoryPort inventoryLedgerRepositoryPort;
    private EventPublisherPort eventPublisherPort;
    private ReserveForOrderUseCase useCase;

    @BeforeEach
    void setUp() {
        inventoryRecordRepositoryPort = mock(InventoryRecordRepositoryPort.class);
        reservationRepositoryPort = mock(ReservationRepositoryPort.class);
        inventoryLedgerRepositoryPort = mock(InventoryLedgerRepositoryPort.class);
        eventPublisherPort = mock(EventPublisherPort.class);
        ReserveInventoryUseCase reserveInventoryUseCase = new ReserveInventoryUseCase(inventoryRecordRepositoryPort,
                reservationRepositoryPort, inventoryLedgerRepositoryPort);
        useCase = new ReserveForOrderUseCase(reserveInventoryUseCase, eventPublisherPort);
        when(reservationRepositoryPort.findByReferenceId(any())).thenReturn(Optional.empty());
        when(reservationRepositoryPort.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void reserveForOrder_reservesEveryLineItem_andPublishesSuccess() {
        when(inventoryRecordRepositoryPort.tryReserve(any(), any(), anyInt())).thenReturn(true);

        useCase.reserveForOrder("order-1",
                List.of(new ReservationLineItem("sku-1", 2), new ReservationLineItem("sku-2", 1)));

        verify(inventoryRecordRepositoryPort).tryReserve("sku-1", "MAIN", 2);
        verify(inventoryRecordRepositoryPort).tryReserve("sku-2", "MAIN", 1);
        verify(reservationRepositoryPort).save(argThat(r -> r.getReferenceId().equals("order-1:sku-1")));
        verify(reservationRepositoryPort).save(argThat(r -> r.getReferenceId().equals("order-1:sku-2")));
        verify(eventPublisherPort).publish(argThat(e ->
                e instanceof InventoryReservedEvent evt && evt.getOrderId().equals("order-1")));
    }

    @Test
    void reserveForOrder_propagatesFailure_withoutPublishingSuccess() {
        when(inventoryRecordRepositoryPort.tryReserve(eq("sku-1"), any(), anyInt())).thenReturn(true);
        when(inventoryRecordRepositoryPort.tryReserve(eq("sku-2"), any(), anyInt())).thenReturn(false);

        assertThatThrownBy(() -> useCase.reserveForOrder("order-1",
                List.of(new ReservationLineItem("sku-1", 2), new ReservationLineItem("sku-2", 1))))
                .isInstanceOf(InsufficientInventoryException.class);

        verify(eventPublisherPort, never()).publish(any());
    }
}
