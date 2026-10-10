package com.nexus.fulfillment.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservationTest {

    @Test
    void create_startsPendingWithAFutureExpiry() {
        Reservation reservation = Reservation.create("order-1:sku-1", "sku-1", "MAIN", 2);

        assertThat(reservation.getId()).isNotBlank();
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.PENDING);
        assertThat(reservation.getExpiresAt()).isAfter(reservation.getCreatedAt());
        assertThat(reservation.getQuantity()).isEqualTo(2);
    }

    @Test
    void commit_movesFromPendingToCommitted() {
        Reservation reservation = Reservation.create("order-1:sku-1", "sku-1", "MAIN", 2);

        Reservation committed = reservation.commit();

        assertThat(committed.getStatus()).isEqualTo(ReservationStatus.COMMITTED);
    }

    @Test
    void release_movesFromPendingToReleased() {
        Reservation reservation = Reservation.create("order-1:sku-1", "sku-1", "MAIN", 2);

        Reservation released = reservation.release();

        assertThat(released.getStatus()).isEqualTo(ReservationStatus.RELEASED);
    }

    @Test
    void commit_rejectsANonPendingReservation() {
        Reservation released = Reservation.create("order-1:sku-1", "sku-1", "MAIN", 2).release();

        assertThatThrownBy(released::commit).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void release_rejectsANonPendingReservation() {
        Reservation committed = Reservation.create("order-1:sku-1", "sku-1", "MAIN", 2).commit();

        assertThatThrownBy(committed::release).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void isExpired_isTrueOnlyWhenStillPendingAndPastExpiry() {
        Reservation reservation = Reservation.reconstitute("r-1", "order-1:sku-1", "sku-1", "MAIN", 2,
                ReservationStatus.PENDING, Instant.now().minus(1, ChronoUnit.HOURS),
                Instant.now().minus(2, ChronoUnit.HOURS));

        assertThat(reservation.isExpired(Instant.now())).isTrue();
        assertThat(reservation.commit().isExpired(Instant.now())).isFalse();
    }
}
