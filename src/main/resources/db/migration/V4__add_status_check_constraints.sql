-- ReservationStatus and LedgerMovementType are enums in Java, but both columns are plain
-- VARCHAR(20). The expiry job and the stock maths act on the exact values, so a row written any
-- other way (a script, a migration, a future service) with a misspelt value would be silently
-- ignored or miscounted. These constraints make the database refuse it.
-- When a value is added to either enum, a new migration must widen the matching constraint.
ALTER TABLE inventory_reservations
    ADD CONSTRAINT chk_inventory_reservations_status
        CHECK (status IN ('PENDING', 'COMMITTED', 'RELEASED'));

ALTER TABLE inventory_ledger
    ADD CONSTRAINT chk_inventory_ledger_movement_type
        CHECK (movement_type IN ('INTAKE', 'RESERVE', 'RELEASE', 'COMMIT'));
