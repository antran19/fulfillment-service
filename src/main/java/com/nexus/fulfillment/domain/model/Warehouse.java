package com.nexus.fulfillment.domain.model;

// SRS allows a fixed warehouse in the MVP phase. warehouseId is still tracked as its own
// column everywhere (not hardcoded into queries) so adding real multi-warehouse support
// later is a data change, not a schema change.
public final class Warehouse {
    public static final String DEFAULT_ID = "MAIN";

    private Warehouse() {
    }
}
