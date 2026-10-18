package com.nexus.fulfillment.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public class Reservation {

    // Default hold, for an order's line items: it must outlive commerce-service's own 30-minute
    // unpaid-order cancel (which releases the hold through OrderCancelled), or a hold could lapse
    // while the buyer can still pay -- the payment's commit would then be a no-op and the stock
    // never taken. It is only the safety net for a hold nobody ever releases. See
    // ExpireReservationsJob. A unit held for an auction passes its own, longer hold.
    public static final Duration DEFAULT_HOLD = Duration.ofMinutes(35);

    private final String id;
    private final String referenceId;
    private final String skuId;
    private final String warehouseId;
    private final int quantity;
    private final ReservationStatus status;
    private final Instant expiresAt;
    private final Instant createdAt;

    private Reservation(String id, String referenceId, String skuId, String warehouseId, int quantity,
                         ReservationStatus status, Instant expiresAt, Instant createdAt) {
        this.id = id;
        this.referenceId = referenceId;
        this.skuId = skuId;
        this.warehouseId = warehouseId;
        this.quantity = quantity;
        this.status = status;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public static Reservation create(String referenceId, String skuId, String warehouseId, int quantity) {
        return create(referenceId, skuId, warehouseId, quantity, DEFAULT_HOLD);
    }

    public static Reservation create(String referenceId, String skuId, String warehouseId, int quantity,
                                     Duration hold) {
        Instant now = Instant.now();
        return new Reservation(UUID.randomUUID().toString(), referenceId, skuId, warehouseId, quantity,
                ReservationStatus.PENDING, now.plus(hold), now);
    }

    public static Reservation reconstitute(String id, String referenceId, String skuId, String warehouseId,
                                            int quantity, ReservationStatus status, Instant expiresAt,
                                            Instant createdAt) {
        return new Reservation(id, referenceId, skuId, warehouseId, quantity, status, expiresAt, createdAt);
    }

    public Reservation commit() {
        requirePending();
        return new Reservation(id, referenceId, skuId, warehouseId, quantity, ReservationStatus.COMMITTED,
                expiresAt, createdAt);
    }

    public Reservation release() {
        requirePending();
        return new Reservation(id, referenceId, skuId, warehouseId, quantity, ReservationStatus.RELEASED,
                expiresAt, createdAt);
    }

    private void requirePending() {
        if (status != ReservationStatus.PENDING) {
            throw new IllegalStateException("Reservation " + id + " is " + status + ", not PENDING");
        }
    }

    public boolean isExpired(Instant now) {
        return status == ReservationStatus.PENDING && now.isAfter(expiresAt);
    }

    public String getId() { return id; }
    public String getReferenceId() { return referenceId; }
    public String getSkuId() { return skuId; }
    public String getWarehouseId() { return warehouseId; }
    public int getQuantity() { return quantity; }
    public ReservationStatus getStatus() { return status; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
}
