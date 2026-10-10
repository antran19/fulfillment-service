package com.nexus.fulfillment.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InventoryLedgerEntryTest {

    @Test
    void record_generatesIdAndStoresFields() {
        InventoryLedgerEntry entry = InventoryLedgerEntry.record(
                LedgerMovementType.RESERVE, "sku-1", "MAIN", 2, "order-1:sku-1");

        assertThat(entry.getId()).isNotBlank();
        assertThat(entry.getMovementType()).isEqualTo(LedgerMovementType.RESERVE);
        assertThat(entry.getSkuId()).isEqualTo("sku-1");
        assertThat(entry.getWarehouseId()).isEqualTo("MAIN");
        assertThat(entry.getQuantity()).isEqualTo(2);
        assertThat(entry.getReferenceId()).isEqualTo("order-1:sku-1");
        assertThat(entry.getOccurredAt()).isNotNull();
    }

    @Test
    void record_allowsANullReferenceIdForIntake() {
        InventoryLedgerEntry entry = InventoryLedgerEntry.record(
                LedgerMovementType.INTAKE, "sku-1", "MAIN", 10, null);

        assertThat(entry.getReferenceId()).isNull();
    }
}
