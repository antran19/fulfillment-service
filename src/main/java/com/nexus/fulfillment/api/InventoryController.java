package com.nexus.fulfillment.api;

import com.nexus.common.core.ApiResponse;
import com.nexus.fulfillment.api.dto.request.IntakeRequest;
import com.nexus.fulfillment.api.dto.request.RestockRequest;
import com.nexus.fulfillment.api.dto.response.InventoryResponse;
import com.nexus.fulfillment.api.mapper.FulfillmentApiMapper;
import com.nexus.fulfillment.application.usecase.GetInventoryUseCase;
import com.nexus.fulfillment.application.usecase.IntakeInventoryUseCase;
import com.nexus.fulfillment.application.usecase.RestockInventoryUseCase;
import com.nexus.fulfillment.domain.model.Warehouse;
import jakarta.validation.Valid;
import com.nexus.common.security.RequiresPrivilege;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private static final SimpleGrantedAuthority MANAGE = new SimpleGrantedAuthority("INVENTORY.MANAGE");

    private final IntakeInventoryUseCase intakeInventoryUseCase;
    private final RestockInventoryUseCase restockInventoryUseCase;
    private final GetInventoryUseCase getInventoryUseCase;
    private final FulfillmentApiMapper mapper;

    public InventoryController(IntakeInventoryUseCase intakeInventoryUseCase,
                                RestockInventoryUseCase restockInventoryUseCase,
                                GetInventoryUseCase getInventoryUseCase, FulfillmentApiMapper mapper) {
        this.intakeInventoryUseCase = intakeInventoryUseCase;
        this.restockInventoryUseCase = restockInventoryUseCase;
        this.getInventoryUseCase = getInventoryUseCase;
        this.mapper = mapper;
    }

    @RequiresPrivilege("INVENTORY.MANAGE")
    @PostMapping("/intake")
    public ResponseEntity<ApiResponse<Void>> intake(@Valid @RequestBody IntakeRequest request) {
        String warehouseId = request.warehouseId() != null ? request.warehouseId() : Warehouse.DEFAULT_ID;
        intakeInventoryUseCase.intake(request.productId(), warehouseId, request.quantity());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // A Seller adds stock to their own product; Admin (INVENTORY.MANAGE) can restock any product.
    // Returns the stock after the change so the seller sees the new numbers.
    @RequiresPrivilege("INVENTORY.RESTOCK")
    @PostMapping("/{productId}/restock")
    public ResponseEntity<ApiResponse<InventoryResponse>> restock(Authentication authentication,
            @PathVariable String productId, @Valid @RequestBody RestockRequest request) {
        restockInventoryUseCase.restock(productId, request.quantity(), (String) authentication.getPrincipal(),
                authentication.getAuthorities().contains(MANAGE));
        return ResponseEntity.ok(ApiResponse.ok(mapper.toResponse(
                getInventoryUseCase.get(productId, Warehouse.DEFAULT_ID))));
    }

    // {productId} is the product's own id (not the SKU id shown in the catalog response).
    @RequiresPrivilege("INVENTORY.VIEW")
    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<InventoryResponse>> get(@PathVariable String productId,
            @RequestParam(required = false) String warehouseId) {
        String resolvedWarehouseId = warehouseId != null ? warehouseId : Warehouse.DEFAULT_ID;
        return ResponseEntity.ok(ApiResponse.ok(mapper.toResponse(getInventoryUseCase.get(productId, resolvedWarehouseId))));
    }
}
