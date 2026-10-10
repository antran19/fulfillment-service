package com.nexus.fulfillment.api.mapper;

import com.nexus.fulfillment.api.dto.response.InventoryResponse;
import com.nexus.fulfillment.api.dto.response.ReservationResponse;
import com.nexus.fulfillment.domain.model.InventoryRecord;
import com.nexus.fulfillment.domain.model.Reservation;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FulfillmentApiMapper {
    InventoryResponse toResponse(InventoryRecord record);
    ReservationResponse toResponse(Reservation reservation);
}
