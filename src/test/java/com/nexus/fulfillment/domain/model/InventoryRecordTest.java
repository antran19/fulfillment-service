package com.nexus.fulfillment.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InventoryRecordTest {

    @Test
    void createEmpty_startsAtZero() {
        InventoryRecord record = InventoryRecord.createEmpty("sku-1", "MAIN");

        assertThat(record.getTotalQuantity()).isZero();
        assertThat(record.getReservedQuantity()).isZero();
        assertThat(record.getUnavailableQuantity()).isZero();
        assertThat(record.getAvailableQuantity()).isZero();
    }

    @Test
    void getAvailableQuantity_isTotalMinusReservedMinusUnavailable() {
        InventoryRecord record = InventoryRecord.reconstitute("sku-1", "MAIN", 10, 3, 1);

        assertThat(record.getAvailableQuantity()).isEqualTo(6);
    }
}
