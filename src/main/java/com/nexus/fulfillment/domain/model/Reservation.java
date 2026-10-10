package com.nexus.fulfillment.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public class Reservation {

    // Checkout/auction-settlement window: long enough to complete payment, short enough
    // that abandoned reservations don't tie up stock for long. See ExpireReservationsJob.
    private static final Duration TTL = Duration.ofMinutes(15);

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
        Instant now = Instant.now();
        return new Reservation(UUID.randomUUID().toString(), referenceId, skuId, warehouseId, quantity,
                ReservationStatus.PENDING, now.plus(TTL), now);
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
