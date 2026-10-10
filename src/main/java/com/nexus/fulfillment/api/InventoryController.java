package com.nexus.fulfillment.api;

import com.nexus.common.core.ApiResponse;
import com.nexus.fulfillment.api.dto.request.IntakeRequest;
import com.nexus.fulfillment.api.dto.response.InventoryResponse;
import com.nexus.fulfillment.api.mapper.FulfillmentApiMapper;
import com.nexus.fulfillment.application.usecase.GetInventoryUseCase;
import com.nexus.fulfillment.application.usecase.IntakeInventoryUseCase;
import com.nexus.fulfillment.domain.model.Warehouse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final IntakeInventoryUseCase intakeInventoryUseCase;
    private final GetInventoryUseCase getInventoryUseCase;
    private final FulfillmentApiMapper mapper;

    public InventoryController(IntakeInventoryUseCase intakeInventoryUseCase, GetInventoryUseCase getInventoryUseCase,
                                FulfillmentApiMapper mapper) {
        this.intakeInventoryUseCase = intakeInventoryUseCase;
        this.getInventoryUseCase = getInventoryUseCase;
        this.mapper = mapper;
    }

    @PostMapping("/intake")
    public ResponseEntity<ApiResponse<Void>> intake(@Valid @RequestBody IntakeRequest request) {
        String warehouseId = request.warehouseId() != null ? request.warehouseId() : Warehouse.DEFAULT_ID;
        intakeInventoryUseCase.intake(request.skuId(), warehouseId, request.quantity());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/{skuId}")
    public ResponseEntity<ApiResponse<InventoryResponse>> get(@PathVariable String skuId,
            @RequestParam(required = false) String warehouseId) {
        String resolvedWarehouseId = warehouseId != null ? warehouseId : Warehouse.DEFAULT_ID;
        return ResponseEntity.ok(ApiResponse.ok(mapper.toResponse(getInventoryUseCase.get(skuId, resolvedWarehouseId))));
    }
}
