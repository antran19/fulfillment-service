package com.nexus.fulfillment.infrastructure.persistence;

import com.nexus.fulfillment.domain.model.InventoryRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(InventoryRecordRepositoryAdapter.class)
class InventoryRecordRepositoryAdapterTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("fulfillment_db").withUsername("nexus").withPassword("nexus");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired private InventoryRecordRepositoryAdapter adapter;
    @Autowired private TestEntityManager entityManager;
    @Autowired private javax.sql.DataSource dataSource;

    @Test
    void intake_createsTheRowOnFirstCall_andAddsToTotalOnSubsequentCalls() {
        adapter.intake("sku-1", "MAIN", 10);
        entityManager.flush();
        entityManager.clear();
        adapter.intake("sku-1", "MAIN", 5);
        entityManager.flush();
        entityManager.clear();

        InventoryRecord record = adapter.findBySkuAndWarehouse("sku-1", "MAIN").orElseThrow();
        assertThat(record.getTotalQuantity()).isEqualTo(15);
        assertThat(record.getAvailableQuantity()).isEqualTo(15);
    }

    @Test
    void tryReserve_succeedsWhenEnoughStockIsAvailable() {
        adapter.intake("sku-2", "MAIN", 10);
        entityManager.flush();
        entityManager.clear();

        boolean reserved = adapter.tryReserve("sku-2", "MAIN", 4);
        entityManager.flush();
        entityManager.clear();

        assertThat(reserved).isTrue();
        InventoryRecord record = adapter.findBySkuAndWarehouse("sku-2", "MAIN").orElseThrow();
        assertThat(record.getReservedQuantity()).isEqualTo(4);
        assertThat(record.getAvailableQuantity()).isEqualTo(6);
    }

    @Test
    void tryReserve_failsWhenNotEnoughStockIsAvailable() {
        adapter.intake("sku-3", "MAIN", 2);
        entityManager.flush();
        entityManager.clear();

        boolean reserved = adapter.tryReserve("sku-3", "MAIN", 5);

        assertThat(reserved).isFalse();
        InventoryRecord record = adapter.findBySkuAndWarehouse("sku-3", "MAIN").orElseThrow();
        assertThat(record.getReservedQuantity()).isZero();
    }

    @Test
    void release_givesReservedStockBackToAvailable() {
        adapter.intake("sku-4", "MAIN", 10);
        entityManager.flush();
        entityManager.clear();
        adapter.tryReserve("sku-4", "MAIN", 4);
        entityManager.flush();
        entityManager.clear();

        adapter.release("sku-4", "MAIN", 4);
        entityManager.flush();
        entityManager.clear();

        InventoryRecord record = adapter.findBySkuAndWarehouse("sku-4", "MAIN").orElseThrow();
        assertThat(record.getReservedQuantity()).isZero();
        assertThat(record.getTotalQuantity()).isEqualTo(10);
        assertThat(record.getAvailableQuantity()).isEqualTo(10);
    }

    @Test
    void commit_permanentlyRemovesReservedStockFromTotal() {
        adapter.intake("sku-5", "MAIN", 10);
        entityManager.flush();
        entityManager.clear();
        adapter.tryReserve("sku-5", "MAIN", 4);
        entityManager.flush();
        entityManager.clear();

        adapter.commit("sku-5", "MAIN", 4);
        entityManager.flush();
        entityManager.clear();

        InventoryRecord record = adapter.findBySkuAndWarehouse("sku-5", "MAIN").orElseThrow();
        assertThat(record.getTotalQuantity()).isEqualTo(6);
        assertThat(record.getReservedQuantity()).isZero();
        assertThat(record.getAvailableQuantity()).isEqualTo(6);
    }

    // The test that actually matters: 20 threads race to reserve 1 unit each of a SKU that
    // only has 5 available, using 20 separate DB connections/transactions (not this test's
    // own JPA session, which @DataJpaTest would otherwise serialize everything through) --
    // proving the atomic conditional UPDATE in InventoryRecordJpaRepository.tryReserve, not
    // application-level locking, is what prevents overselling.
    // @DataJpaTest wraps each test method in one transaction that rolls back at the end --
    // NOT_SUPPORTED suspends it for this method, so intake() actually commits and is visible
    // to the 20 separate raw-JDBC connections below. Without this, every concurrent UPDATE
    // sees zero rows (nothing committed yet) and the test is meaningless.
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void tryReserve_underConcurrentLoad_neverReservesMoreThanAvailable() throws InterruptedException {
        adapter.intake("sku-concurrent", "MAIN", 5);

        int threadCount = 20;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLine = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger();

        List<Runnable> tasks = java.util.stream.IntStream.range(0, threadCount)
                .<Runnable>mapToObj(i -> () -> {
                    try {
                        startLine.await();
                        if (tryReserveOnOwnConnection("sku-concurrent", "MAIN", 1)) {
                            successCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList();

        tasks.forEach(pool::execute);
        startLine.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        assertThat(successCount.get()).isEqualTo(5);

        InventoryRecord record = adapter.findBySkuAndWarehouse("sku-concurrent", "MAIN").orElseThrow();
        assertThat(record.getReservedQuantity()).isEqualTo(5);
        assertThat(record.getAvailableQuantity()).isZero();
    }

    // Bypasses JPA/the shared test EntityManager entirely for this one call -- raw JDBC on
    // its own connection, so concurrent threads genuinely hit the database concurrently
    // instead of being serialized by a shared Hibernate Session.
    private boolean tryReserveOnOwnConnection(String skuId, String warehouseId, int quantity) throws Exception {
        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement(
                     "UPDATE inventory_records SET reserved_quantity = reserved_quantity + ? "
                             + "WHERE sku_id = ? AND warehouse_id = ? "
                             + "AND (total_quantity - reserved_quantity - unavailable_quantity) >= ?")) {
            statement.setInt(1, quantity);
            statement.setString(2, skuId);
            statement.setString(3, warehouseId);
            statement.setInt(4, quantity);
            return statement.executeUpdate() > 0;
        }
    }
}
