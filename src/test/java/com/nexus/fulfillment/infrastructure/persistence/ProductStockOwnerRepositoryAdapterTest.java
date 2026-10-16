package com.nexus.fulfillment.infrastructure.persistence;

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
@Import(ProductStockOwnerRepositoryAdapter.class)
class ProductStockOwnerRepositoryAdapterTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("fulfillment_db").withUsername("nexus").withPassword("nexus");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired private ProductStockOwnerRepositoryAdapter adapter;
    @Autowired private TestEntityManager entityManager;

    @Test
    void registerIfAbsent_createsTheOwnerAndTheNextCallReportsItAlreadyExisted() {
        assertThat(adapter.registerIfAbsent("p-1", "seller-1")).isTrue();
        entityManager.flush();
        entityManager.clear();

        assertThat(adapter.registerIfAbsent("p-1", "seller-1")).isFalse();
        assertThat(adapter.findSellerId("p-1")).contains("seller-1");
    }

    @Test
    void registerIfAbsent_doesNotOverwriteTheFirstOwner() {
        adapter.registerIfAbsent("p-2", "seller-1");
        entityManager.flush();
        entityManager.clear();

        assertThat(adapter.registerIfAbsent("p-2", "seller-2")).isFalse();
        assertThat(adapter.findSellerId("p-2")).contains("seller-1");
    }

    @Test
    void findSellerId_isEmptyForAnUnknownProduct() {
        assertThat(adapter.findSellerId("unknown")).isEmpty();
    }
}
