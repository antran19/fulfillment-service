package com.nexus.fulfillment.application.usecase;

import com.nexus.common.events.ReservationLineItem;
import com.nexus.fulfillment.domain.model.Warehouse;

import java.util.List;

// Triggered by OrderReturnReceivedEvent -- commerce-service's Admin/Support Staff confirmed
// a shipped order's goods actually came back, so restock every line item into the default
// warehouse. Unlike ReserveForOrderUseCase there's no all-or-nothing concern here: intake
// can't fail the way a reservation can (no stock ceiling to hit).
public class IntakeForReturnUseCase {

    private final IntakeInventoryUseCase intakeInventoryUseCase;

    public IntakeForReturnUseCase(IntakeInventoryUseCase intakeInventoryUseCase) {
        this.intakeInventoryUseCase = intakeInventoryUseCase;
    }

    public void intakeForReturn(List<ReservationLineItem> items) {
        for (ReservationLineItem item : items) {
            intakeInventoryUseCase.intake(item.skuId(), Warehouse.DEFAULT_ID, item.quantity());
        }
    }
}
