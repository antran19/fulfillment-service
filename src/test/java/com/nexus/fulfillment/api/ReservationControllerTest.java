package com.nexus.fulfillment.api;

import com.nexus.common.security.JwtAuthenticationFilter;
import com.nexus.common.security.JwtTokenProvider;
import com.nexus.common.web.GlobalExceptionHandler;
import com.nexus.fulfillment.api.mapper.FulfillmentApiMapperImpl;
import com.nexus.fulfillment.application.exception.InsufficientInventoryException;
import com.nexus.fulfillment.application.usecase.CommitReservationUseCase;
import com.nexus.fulfillment.application.usecase.GetReservationUseCase;
import com.nexus.fulfillment.application.usecase.ReleaseReservationUseCase;
import com.nexus.fulfillment.application.usecase.ReserveInventoryUseCase;
import com.nexus.fulfillment.domain.model.Reservation;
import com.nexus.fulfillment.infrastructure.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReservationController.class)
@Import({GlobalExceptionHandler.class, FulfillmentApiMapperImpl.class, SecurityConfig.class,
        JwtAuthenticationFilter.class, JwtTokenProvider.class,
        com.nexus.common.security.PrivilegeAuthorizationAspect.class})
@org.springframework.boot.autoconfigure.ImportAutoConfiguration(org.springframework.boot.autoconfigure.aop.AopAutoConfiguration.class)
class ReservationControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    @MockBean private ReserveInventoryUseCase reserveInventoryUseCase;
    @MockBean private CommitReservationUseCase commitReservationUseCase;
    @MockBean private ReleaseReservationUseCase releaseReservationUseCase;
    @MockBean private GetReservationUseCase getReservationUseCase;

    private String token() {
        return jwtTokenProvider.generateToken("caller-1", "SELLER", List.of("INVENTORY.MANAGE", "INVENTORY.VIEW"), "TRUSTED");
    }

    @Test
    void reserve_returns201WithReservation() throws Exception {
        when(reserveInventoryUseCase.reserve("order-1:sku-1", "sku-1", "MAIN", 2))
                .thenReturn(Reservation.create("order-1:sku-1", "sku-1", "MAIN", 2));

        mockMvc.perform(post("/api/v1/reservations")
                        .header("Authorization", "Bearer " + token())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"referenceId":"order-1:sku-1","productId":"sku-1","warehouseId":"MAIN","quantity":2}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.referenceId").value("order-1:sku-1"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void reserve_returns409WhenInsufficientStock() throws Exception {
        when(reserveInventoryUseCase.reserve(any(), any(), any(), anyInt()))
                .thenThrow(new InsufficientInventoryException("sku-1", "MAIN", 2));

        mockMvc.perform(post("/api/v1/reservations")
                        .header("Authorization", "Bearer " + token())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"referenceId":"order-1:sku-1","productId":"sku-1","warehouseId":"MAIN","quantity":2}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INSUFFICIENT_INVENTORY"));
    }

    @Test
    void reserve_returns401WithoutAToken() throws Exception {
        mockMvc.perform(post("/api/v1/reservations")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"referenceId":"order-1:sku-1","productId":"sku-1","quantity":2}"""))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void commit_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/reservations/r-1/commit").header("Authorization", "Bearer " + token()))
                .andExpect(status().isOk());
    }

    @Test
    void release_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/reservations/r-1/release").header("Authorization", "Bearer " + token()))
                .andExpect(status().isOk());
    }

    @Test
    void view_returns403WithoutTheViewPrivilege() throws Exception {
        String noPrivileges = jwtTokenProvider.generateToken("caller-1", "BUYER", List.of(), "TRUSTED");

        mockMvc.perform(get("/api/v1/reservations/res-1").header("Authorization", "Bearer " + noPrivileges))
                .andExpect(status().isForbidden());
    }

    @Test
    void manage_returns403WithoutTheManagePrivilege() throws Exception {
        String viewOnly = jwtTokenProvider.generateToken("caller-1", "SELLER", List.of("INVENTORY.VIEW"), "TRUSTED");

        mockMvc.perform(post("/api/v1/reservations/res-1/commit").header("Authorization", "Bearer " + viewOnly)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }
}
