package com.nexus.fulfillment.infrastructure.persistence;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// V4: the database itself refuses values the application already forbids.
// No surrounding transaction: each insert must commit (or fail) on its own.
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class FulfillmentCheckConstraintsTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("fulfillment_db").withUsername("nexus").withPassword("nexus");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired private JdbcTemplate jdbcTemplate;

    private void insertReservation(String status) {
        jdbcTemplate.update("INSERT INTO inventory_reservations (id, reference_id, sku_id, warehouse_id, quantity, status, expires_at, created_at) "
                + "VALUES (?, ?, 'sku-1', 'wh-1', 1, ?, now() + interval '1 hour', now())",
                UUID.randomUUID(), "ref-" + UUID.randomUUID(), status);
    }

    private void insertLedger(String movementType) {
        jdbcTemplate.update("INSERT INTO inventory_ledger (id, sku_id, warehouse_id, movement_type, quantity, occurred_at) "
                + "VALUES (?, 'sku-1', 'wh-1', ?, 1, now())", UUID.randomUUID(), movementType);
    }

    @ParameterizedTest
    @ValueSource(strings = {"PENDING", "COMMITTED", "RELEASED"})
    void everyReservationStatusTheApplicationKnows_isAccepted(String status) {
        assertThatCode(() -> insertReservation(status)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ABC", "pending", "EXPIRED", ""})
    void aReservationStatusOutsideTheEnum_isRejected(String status) {
        assertThatThrownBy(() -> insertReservation(status))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("chk_inventory_reservations_status");
    }

    @ParameterizedTest
    @ValueSource(strings = {"INTAKE", "RESERVE", "RELEASE", "COMMIT"})
    void everyMovementTypeTheApplicationKnows_isAccepted(String type) {
        assertThatCode(() -> insertLedger(type)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ABC", "intake", "SELL", ""})
    void aMovementTypeOutsideTheEnum_isRejected(String type) {
        assertThatThrownBy(() -> insertLedger(type))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("chk_inventory_ledger_movement_type");
    }
}
