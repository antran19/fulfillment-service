package com.nexus.fulfillment.api;

import com.nexus.common.core.ApiResponse;
import com.nexus.fulfillment.api.dto.request.ReserveRequest;
import com.nexus.fulfillment.api.dto.response.ReservationResponse;
import com.nexus.fulfillment.api.mapper.FulfillmentApiMapper;
import com.nexus.fulfillment.application.usecase.CommitReservationUseCase;
import com.nexus.fulfillment.application.usecase.GetReservationUseCase;
import com.nexus.fulfillment.application.usecase.ReleaseReservationUseCase;
import com.nexus.fulfillment.application.usecase.ReserveInventoryUseCase;
import com.nexus.fulfillment.domain.model.Reservation;
import com.nexus.fulfillment.domain.model.Warehouse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import com.nexus.common.security.RequiresPrivilege;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reservations")
public class ReservationController {

    private final ReserveInventoryUseCase reserveInventoryUseCase;
    private final CommitReservationUseCase commitReservationUseCase;
    private final ReleaseReservationUseCase releaseReservationUseCase;
    private final GetReservationUseCase getReservationUseCase;
    private final FulfillmentApiMapper mapper;

    public ReservationController(ReserveInventoryUseCase reserveInventoryUseCase,
                                  CommitReservationUseCase commitReservationUseCase,
                                  ReleaseReservationUseCase releaseReservationUseCase,
                                  GetReservationUseCase getReservationUseCase, FulfillmentApiMapper mapper) {
        this.reserveInventoryUseCase = reserveInventoryUseCase;
        this.commitReservationUseCase = commitReservationUseCase;
        this.releaseReservationUseCase = releaseReservationUseCase;
        this.getReservationUseCase = getReservationUseCase;
        this.mapper = mapper;
    }

    @RequiresPrivilege("INVENTORY.MANAGE")
    @PostMapping
    public ResponseEntity<ApiResponse<ReservationResponse>> reserve(@Valid @RequestBody ReserveRequest request) {
        String warehouseId = request.warehouseId() != null ? request.warehouseId() : Warehouse.DEFAULT_ID;
        Reservation reservation = reserveInventoryUseCase.reserve(request.referenceId(), request.productId(),
                warehouseId, request.quantity());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(mapper.toResponse(reservation)));
    }

    @RequiresPrivilege("INVENTORY.VIEW")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReservationResponse>> get(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(mapper.toResponse(getReservationUseCase.get(id))));
    }

    @RequiresPrivilege("INVENTORY.MANAGE")
    @PostMapping("/{id}/commit")
    public ResponseEntity<ApiResponse<Void>> commit(@PathVariable String id) {
        commitReservationUseCase.commit(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @RequiresPrivilege("INVENTORY.MANAGE")
    @PostMapping("/{id}/release")
    public ResponseEntity<ApiResponse<Void>> release(@PathVariable String id) {
        releaseReservationUseCase.release(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
