package com.nexus.fulfillment.application.usecase;

import com.nexus.common.core.exception.ValidationException;
import com.nexus.fulfillment.application.port.out.InventoryLedgerRepositoryPort;
import com.nexus.fulfillment.application.port.out.InventoryRecordRepositoryPort;
import com.nexus.fulfillment.domain.model.LedgerMovementType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class IntakeInventoryUseCaseTest {

    private final InventoryLevelNotifier inventoryLevelNotifier = mock(InventoryLevelNotifier.class);

    private InventoryRecordRepositoryPort inventoryRecordRepositoryPort;
    private InventoryLedgerRepositoryPort inventoryLedgerRepositoryPort;
    private IntakeInventoryUseCase useCase;

    @BeforeEach
    void setUp() {
        inventoryRecordRepositoryPort = mock(InventoryRecordRepositoryPort.class);
        inventoryLedgerRepositoryPort = mock(InventoryLedgerRepositoryPort.class);
        useCase = new IntakeInventoryUseCase(inventoryRecordRepositoryPort, inventoryLedgerRepositoryPort,
                inventoryLevelNotifier);
    }

    @Test
    void intake_addsStockAndRecordsLedgerEntry() {
        useCase.intake("sku-1", "MAIN", 10);

        verify(inventoryRecordRepositoryPort).intake("sku-1", "MAIN", 10);
        verify(inventoryLedgerRepositoryPort).record(argThat(e ->
                e.getMovementType() == LedgerMovementType.INTAKE && e.getSkuId().equals("sku-1")
                        && e.getQuantity() == 10 && e.getReferenceId() == null));
        verify(inventoryLevelNotifier).stockChanged("sku-1", "MAIN");
    }

    @Test
    void intake_rejectsZeroOrNegativeQuantity() {
        assertThatThrownBy(() -> useCase.intake("sku-1", "MAIN", 0)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> useCase.intake("sku-1", "MAIN", -1)).isInstanceOf(ValidationException.class);

        verify(inventoryRecordRepositoryPort, never()).intake(any(), any(), anyInt());
        verify(inventoryLevelNotifier, never()).stockChanged(any(), any());
    }
}
