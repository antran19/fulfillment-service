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
}
