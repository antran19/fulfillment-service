package com.nexus.fulfillment.application.exception;

import com.nexus.common.core.exception.NotFoundException;

public class ReservationNotFoundException extends NotFoundException {
    public ReservationNotFoundException(String id) {
        super("RESERVATION_NOT_FOUND", "Reservation not found: " + id);
    }
}
