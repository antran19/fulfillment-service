package com.nexus.fulfillment.application.usecase;

import com.nexus.common.events.ReservationLineItem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class IntakeForReturnUseCaseTest {

    private final IntakeInventoryUseCase intakeInventoryUseCase = mock(IntakeInventoryUseCase.class);
    private final IntakeForReturnUseCase useCase = new IntakeForReturnUseCase(intakeInventoryUseCase);

    @Test
    void intakeForReturn_intakesEveryLineItemIntoTheDefaultWarehouse() {
        useCase.intakeForReturn(List.of(new ReservationLineItem("sku-1", 2), new ReservationLineItem("sku-2", 1)));

        verify(intakeInventoryUseCase).intake("sku-1", "MAIN", 2);
        verify(intakeInventoryUseCase).intake("sku-2", "MAIN", 1);
    }
}
