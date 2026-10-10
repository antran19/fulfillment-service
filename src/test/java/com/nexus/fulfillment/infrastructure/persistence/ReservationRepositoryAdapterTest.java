package com.nexus.fulfillment.infrastructure.persistence;

import com.nexus.fulfillment.domain.model.Reservation;
import com.nexus.fulfillment.domain.model.ReservationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ReservationRepositoryAdapter.class)
class ReservationRepositoryAdapterTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("fulfillment_db").withUsername("nexus").withPassword("nexus");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired private ReservationRepositoryAdapter adapter;
    @Autowired private TestEntityManager entityManager;

    @Test
    void save_thenFindById_roundTripsFields() {
        Reservation reservation = Reservation.create("order-1:sku-1", "sku-1", "MAIN", 3);
        adapter.save(reservation);
        entityManager.flush();
        entityManager.clear();

        Reservation found = adapter.findById(reservation.getId()).orElseThrow();

        assertThat(found.getReferenceId()).isEqualTo("order-1:sku-1");
        assertThat(found.getQuantity()).isEqualTo(3);
        assertThat(found.getStatus()).isEqualTo(ReservationStatus.PENDING);
    }

    @Test
    void findByReferenceId_locatesTheReservation() {
        Reservation reservation = Reservation.create("order-2:sku-1", "sku-1", "MAIN", 1);
        adapter.save(reservation);
        entityManager.flush();
        entityManager.clear();

        assertThat(adapter.findByReferenceId("order-2:sku-1")).isPresent();
        assertThat(adapter.findByReferenceId("never-reserved")).isEmpty();
    }

    @Test
    void findByOrderId_returnsEveryLineItemReservationForThatOrder() {
        Reservation lineItem1 = Reservation.create("order-9:sku-1", "sku-1", "MAIN", 1);
        Reservation lineItem2 = Reservation.create("order-9:sku-2", "sku-2", "MAIN", 2);
        Reservation otherOrder = Reservation.create("order-10:sku-1", "sku-1", "MAIN", 1);
        adapter.save(lineItem1);
        adapter.save(lineItem2);
        adapter.save(otherOrder);
        entityManager.flush();
        entityManager.clear();

        List<Reservation> found = adapter.findByOrderId("order-9");

        assertThat(found).extracting(Reservation::getId)
                .containsExactlyInAnyOrder(lineItem1.getId(), lineItem2.getId());
    }

    @Test
    void findById_isEmptyForANonUuidId() {
        assertThat(adapter.findById("not-a-uuid")).isEmpty();
    }

    @Test
    void findExpiredPending_onlyReturnsPendingReservationsPastTheirExpiry() {
        Reservation expiredPending = Reservation.reconstitute("11111111-1111-1111-1111-111111111111",
                "order-3:sku-1", "sku-1", "MAIN", 1, ReservationStatus.PENDING,
                Instant.now().minus(1, ChronoUnit.MINUTES), Instant.now().minus(20, ChronoUnit.MINUTES));
        Reservation stillValid = Reservation.create("order-4:sku-1", "sku-1", "MAIN", 1);
        Reservation expiredButCommitted = Reservation.reconstitute("22222222-2222-2222-2222-222222222222",
                "order-5:sku-1", "sku-1", "MAIN", 1, ReservationStatus.COMMITTED,
                Instant.now().minus(1, ChronoUnit.MINUTES), Instant.now().minus(20, ChronoUnit.MINUTES));
        adapter.save(expiredPending);
        adapter.save(stillValid);
        adapter.save(expiredButCommitted);
        entityManager.flush();
        entityManager.clear();

        List<Reservation> expired = adapter.findExpiredPending(Instant.now());

        assertThat(expired).extracting(Reservation::getId).containsExactly(expiredPending.getId());
    }
}
