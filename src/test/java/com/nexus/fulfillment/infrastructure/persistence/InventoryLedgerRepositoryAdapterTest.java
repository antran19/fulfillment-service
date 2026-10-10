package com.nexus.fulfillment.infrastructure.persistence;

import com.nexus.fulfillment.domain.model.InventoryLedgerEntry;
import com.nexus.fulfillment.domain.model.LedgerMovementType;
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

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(InventoryLedgerRepositoryAdapter.class)
class InventoryLedgerRepositoryAdapterTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("fulfillment_db").withUsername("nexus").withPassword("nexus");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired private InventoryLedgerRepositoryAdapter adapter;
    @Autowired private InventoryLedgerJpaRepository jpaRepository;
    @Autowired private TestEntityManager entityManager;

    @Test
    void record_persistsTheEntry() {
        adapter.record(InventoryLedgerEntry.record(LedgerMovementType.RESERVE, "sku-1", "MAIN", 2, "order-1:sku-1"));
        entityManager.flush();
        entityManager.clear();

        assertThat(jpaRepository.findAll()).hasSize(1);
        assertThat(jpaRepository.findAll().get(0).getMovementType()).isEqualTo("RESERVE");
    }
}
