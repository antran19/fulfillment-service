package com.nexus.fulfillment.application.usecase;

import com.nexus.common.core.exception.ForbiddenException;
import com.nexus.fulfillment.application.port.out.ProductStockOwnerRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RestockInventoryUseCaseTest {

    private ProductStockOwnerRepositoryPort ownerPort;
    private IntakeInventoryUseCase intakeInventoryUseCase;
    private RestockInventoryUseCase useCase;

    @BeforeEach
    void setUp() {
        ownerPort = mock(ProductStockOwnerRepositoryPort.class);
        intakeInventoryUseCase = mock(IntakeInventoryUseCase.class);
        useCase = new RestockInventoryUseCase(ownerPort, intakeInventoryUseCase);
    }

    @Test
    void restock_addsStockForTheProductsOwner() {
        when(ownerPort.findSellerId("p-1")).thenReturn(Optional.of("seller-1"));

        useCase.restock("p-1", 5, "seller-1", false);

        verify(intakeInventoryUseCase).intake("p-1", "MAIN", 5);
    }

    @Test
    void restock_rejectsASellerWhoDoesNotOwnTheProduct() {
        when(ownerPort.findSellerId("p-1")).thenReturn(Optional.of("seller-1"));

        assertThatThrownBy(() -> useCase.restock("p-1", 5, "seller-2", false))
                .isInstanceOf(ForbiddenException.class);
        verify(intakeInventoryUseCase, never()).intake(anyString(), anyString(), anyInt());
    }

    @Test
    void restock_rejectsAProductWithNoRecordedOwnerForASeller() {
        when(ownerPort.findSellerId("p-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.restock("p-1", 5, "seller-1", false))
                .isInstanceOf(ForbiddenException.class);
        verify(intakeInventoryUseCase, never()).intake(anyString(), anyString(), anyInt());
    }

    @Test
    void restock_letsAnAdminRestockAnyProductEvenWithoutAnOwner() {
        when(ownerPort.findSellerId("p-1")).thenReturn(Optional.empty());

        useCase.restock("p-1", 5, "admin-1", true);

        verify(intakeInventoryUseCase).intake("p-1", "MAIN", 5);
        verify(ownerPort, never()).findSellerId(anyString());
    }
}
