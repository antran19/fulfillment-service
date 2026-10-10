package com.nexus.fulfillment.application.port.out;

import com.nexus.fulfillment.domain.model.Reservation;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ReservationRepositoryPort {
    Reservation save(Reservation reservation);
    Optional<Reservation> findById(String id);
    Optional<Reservation> findByReferenceId(String referenceId);
    List<Reservation> findExpiredPending(Instant now);

    // Order-level reservations are always made with referenceId = "<orderId>:<skuId>" (see
    // ReserveForOrderUseCase) -- this is how later, separate events (OrderPaid/OrderCancelled,
    // which only carry orderId) can still find every line item's reservation for that order.
    List<Reservation> findByOrderId(String orderId);
}
