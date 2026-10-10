-- MVP phase: warehouse_id is tracked as a real column everywhere (SRS allows a fixed
-- warehouse for now), so adding real multi-warehouse support later is a data change, not a
-- schema change.
CREATE TABLE inventory_records (
    id UUID PRIMARY KEY,
    sku_id VARCHAR(255) NOT NULL,
    warehouse_id VARCHAR(100) NOT NULL,
    total_quantity INTEGER NOT NULL DEFAULT 0,
    reserved_quantity INTEGER NOT NULL DEFAULT 0,
    unavailable_quantity INTEGER NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX idx_inventory_records_sku_warehouse ON inventory_records (sku_id, warehouse_id);

-- reference_id is the caller's idempotency key (e.g. "<orderId>:<skuId>") -- one reservation
-- per logical request, enforced by this unique index (see ReserveInventoryUseCase).
CREATE TABLE inventory_reservations (
    id UUID PRIMARY KEY,
    reference_id VARCHAR(255) NOT NULL,
    sku_id VARCHAR(255) NOT NULL,
    warehouse_id VARCHAR(100) NOT NULL,
    quantity INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX idx_inventory_reservations_reference_id ON inventory_reservations (reference_id);
CREATE INDEX idx_inventory_reservations_status_expires ON inventory_reservations (status, expires_at);

-- Immutable audit trail -- rows are only ever inserted, never updated (see
-- InventoryLedgerEntry).
CREATE TABLE inventory_ledger (
    id UUID PRIMARY KEY,
    sku_id VARCHAR(255) NOT NULL,
    warehouse_id VARCHAR(100) NOT NULL,
    movement_type VARCHAR(20) NOT NULL,
    quantity INTEGER NOT NULL,
    reference_id VARCHAR(255),
    occurred_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_inventory_ledger_sku_warehouse ON inventory_ledger (sku_id, warehouse_id);
