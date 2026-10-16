package com.nexus.fulfillment.api.mapper;

import com.nexus.fulfillment.api.dto.response.InventoryResponse;
import com.nexus.fulfillment.api.dto.response.ReservationResponse;
import com.nexus.fulfillment.domain.model.InventoryRecord;
import com.nexus.fulfillment.domain.model.Reservation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FulfillmentApiMapper {
    // Stock is keyed by the PRODUCT id even though the domain and database call the column
    // skuId (every product has exactly one SKU): the REST API says what the value really is.
    @Mapping(source = "skuId", target = "productId")
    InventoryResponse toResponse(InventoryRecord record);

    @Mapping(source = "skuId", target = "productId")
    ReservationResponse toResponse(Reservation reservation);
}
