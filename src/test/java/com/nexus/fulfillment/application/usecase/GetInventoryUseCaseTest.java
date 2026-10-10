package com.nexus.fulfillment.application.usecase;

import com.nexus.fulfillment.application.port.out.InventoryRecordRepositoryPort;
import com.nexus.fulfillment.domain.model.InventoryRecord;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetInventoryUseCaseTest {

    private final InventoryRecordRepositoryPort inventoryRecordRepositoryPort = mock(InventoryRecordRepositoryPort.class);
    private final GetInventoryUseCase useCase = new GetInventoryUseCase(inventoryRecordRepositoryPort);

    @Test
    void get_returnsTheRecordWhenItExists() {
        when(inventoryRecordRepositoryPort.findBySkuAndWarehouse("sku-1", "MAIN"))
                .thenReturn(Optional.of(InventoryRecord.reconstitute("sku-1", "MAIN", 10, 2, 0)));

        InventoryRecord result = useCase.get("sku-1", "MAIN");

        assertThat(result.getAvailableQuantity()).isEqualTo(8);
    }

    @Test
    void get_returnsAnEmptyRecordWhenNoneExistsYet() {
        when(inventoryRecordRepositoryPort.findBySkuAndWarehouse("sku-new", "MAIN")).thenReturn(Optional.empty());

        InventoryRecord result = useCase.get("sku-new", "MAIN");

        assertThat(result.getTotalQuantity()).isZero();
        assertThat(result.getAvailableQuantity()).isZero();
    }
}
