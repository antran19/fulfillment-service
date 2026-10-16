package com.nexus.fulfillment.application.usecase;

import com.nexus.fulfillment.application.port.out.ProductStockOwnerRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegisterProductStockUseCaseTest {

    private ProductStockOwnerRepositoryPort ownerPort;
    private IntakeInventoryUseCase intakeInventoryUseCase;
    private RegisterProductStockUseCase useCase;

    @BeforeEach
    void setUp() {
        ownerPort = mock(ProductStockOwnerRepositoryPort.class);
        intakeInventoryUseCase = mock(IntakeInventoryUseCase.class);
        useCase = new RegisterProductStockUseCase(ownerPort, intakeInventoryUseCase);
    }

    @Test
    void register_recordsTheOwnerAndOpensInventoryWithTheInitialStock() {
        when(ownerPort.registerIfAbsent("p-1", "seller-1")).thenReturn(true);

        useCase.register("p-1", "seller-1", 25);

        verify(ownerPort).registerIfAbsent("p-1", "seller-1");
        verify(intakeInventoryUseCase).intake("p-1", "MAIN", 25);
    }

    @Test
    void register_aRedeliveredEventDoesNotAddTheInitialStockAgain() {
        when(ownerPort.registerIfAbsent("p-1", "seller-1")).thenReturn(false);

        useCase.register("p-1", "seller-1", 25);

        verify(intakeInventoryUseCase, never()).intake(anyString(), anyString(), anyInt());
    }

    @Test
    void register_withZeroInitialStockOnlyRecordsTheOwner() {
        when(ownerPort.registerIfAbsent("p-1", "seller-1")).thenReturn(true);

        useCase.register("p-1", "seller-1", 0);

        verify(ownerPort).registerIfAbsent("p-1", "seller-1");
        verify(intakeInventoryUseCase, never()).intake(anyString(), anyString(), anyInt());
    }

    @Test
    void register_ignoresAnEventWithoutASeller() {
        useCase.register("p-1", null, 25);
        useCase.register("p-1", " ", 25);

        verify(ownerPort, never()).registerIfAbsent(anyString(), anyString());
        verify(intakeInventoryUseCase, never()).intake(anyString(), anyString(), anyInt());
    }
}
