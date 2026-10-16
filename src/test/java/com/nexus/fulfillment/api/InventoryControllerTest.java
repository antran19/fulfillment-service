package com.nexus.fulfillment.api;

import com.nexus.common.security.JwtAuthenticationFilter;
import com.nexus.common.security.JwtTokenProvider;
import com.nexus.common.web.GlobalExceptionHandler;
import com.nexus.fulfillment.api.mapper.FulfillmentApiMapperImpl;
import com.nexus.fulfillment.application.usecase.GetInventoryUseCase;
import com.nexus.fulfillment.application.usecase.IntakeInventoryUseCase;
import com.nexus.fulfillment.application.usecase.RestockInventoryUseCase;
import com.nexus.fulfillment.domain.model.InventoryRecord;
import com.nexus.fulfillment.infrastructure.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InventoryController.class)
@Import({GlobalExceptionHandler.class, FulfillmentApiMapperImpl.class, SecurityConfig.class,
        JwtAuthenticationFilter.class, JwtTokenProvider.class,
        com.nexus.common.security.PrivilegeAuthorizationAspect.class})
@org.springframework.boot.autoconfigure.ImportAutoConfiguration(org.springframework.boot.autoconfigure.aop.AopAutoConfiguration.class)
class InventoryControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    @MockBean private IntakeInventoryUseCase intakeInventoryUseCase;
    @MockBean private GetInventoryUseCase getInventoryUseCase;
    @MockBean private RestockInventoryUseCase restockInventoryUseCase;

    private String token() {
        return jwtTokenProvider.generateToken("caller-1", "ADMIN", List.of("INVENTORY.MANAGE", "INVENTORY.VIEW"), "TRUSTED");
    }

    @Test
    void intake_returns200_andDefaultsWarehouseWhenOmitted() throws Exception {
        mockMvc.perform(post("/api/v1/inventory/intake")
                        .header("Authorization", "Bearer " + token())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"productId":"sku-1","quantity":10}"""))
                .andExpect(status().isOk());

        verify(intakeInventoryUseCase).intake("sku-1", "MAIN", 10);
    }

    @Test
    void get_returns200WithInventory() throws Exception {
        when(getInventoryUseCase.get("sku-1", "MAIN")).thenReturn(InventoryRecord.reconstitute("sku-1", "MAIN", 10, 3, 0));

        mockMvc.perform(get("/api/v1/inventory/sku-1").header("Authorization", "Bearer " + token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalQuantity").value(10))
                .andExpect(jsonPath("$.data.availableQuantity").value(7));
    }

    @Test
    void intake_returns400ForZeroQuantity() throws Exception {
        mockMvc.perform(post("/api/v1/inventory/intake")
                        .header("Authorization", "Bearer " + token())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"productId":"sku-1","quantity":0}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void view_returns403WithoutTheViewPrivilege() throws Exception {
        String noPrivileges = jwtTokenProvider.generateToken("caller-1", "BUYER", List.of(), "TRUSTED");

        mockMvc.perform(get("/api/v1/inventory/sku-1").header("Authorization", "Bearer " + noPrivileges))
                .andExpect(status().isForbidden());
    }

    @Test
    void manage_returns403WithoutTheManagePrivilege() throws Exception {
        String viewOnly = jwtTokenProvider.generateToken("caller-1", "SELLER", List.of("INVENTORY.VIEW"), "TRUSTED");

        mockMvc.perform(post("/api/v1/inventory/intake").header("Authorization", "Bearer " + viewOnly)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content("{\"productId\":\"sku-1\",\"quantity\":1}"))
                .andExpect(status().isForbidden());
    }

    private String sellerToken() {
        return jwtTokenProvider.generateToken("seller-1", "SELLER", List.of("INVENTORY.RESTOCK"), "TRUSTED");
    }

    @Test
    void restock_returns200ForASellerAndPassesTheCallerWithoutManageScope() throws Exception {
        when(getInventoryUseCase.get("p-1", "MAIN")).thenReturn(InventoryRecord.reconstitute("p-1", "MAIN", 15, 0, 0));

        mockMvc.perform(post("/api/v1/inventory/p-1/restock")
                        .header("Authorization", "Bearer " + sellerToken())
                        .contentType(APPLICATION_JSON)
                        .content("{\"quantity\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.productId").value("p-1"))
                .andExpect(jsonPath("$.data.totalQuantity").value(15));

        verify(restockInventoryUseCase).restock("p-1", 5, "seller-1", false);
    }

    @Test
    void restock_givesAdminManageScope() throws Exception {
        String admin = jwtTokenProvider.generateToken("admin-1", "ADMIN",
                List.of("INVENTORY.RESTOCK", "INVENTORY.MANAGE"), "TRUSTED");
        when(getInventoryUseCase.get("p-1", "MAIN")).thenReturn(InventoryRecord.reconstitute("p-1", "MAIN", 5, 0, 0));

        mockMvc.perform(post("/api/v1/inventory/p-1/restock")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(APPLICATION_JSON)
                        .content("{\"quantity\":5}"))
                .andExpect(status().isOk());

        verify(restockInventoryUseCase).restock("p-1", 5, "admin-1", true);
    }

    @Test
    void restock_returns403WithoutTheRestockPrivilege() throws Exception {
        String buyer = jwtTokenProvider.generateToken("buyer-1", "BUYER", List.of(), "TRUSTED");

        mockMvc.perform(post("/api/v1/inventory/p-1/restock")
                        .header("Authorization", "Bearer " + buyer)
                        .contentType(APPLICATION_JSON)
                        .content("{\"quantity\":5}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void restock_returns400ForAQuantityBelowOne() throws Exception {
        mockMvc.perform(post("/api/v1/inventory/p-1/restock")
                        .header("Authorization", "Bearer " + sellerToken())
                        .contentType(APPLICATION_JSON)
                        .content("{\"quantity\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void restock_returns403WhenTheCallerDoesNotOwnTheProduct() throws Exception {
        org.mockito.Mockito.doThrow(new com.nexus.common.core.exception.ForbiddenException("NOT_THE_PRODUCT_OWNER", "no"))
                .when(restockInventoryUseCase).restock("p-1", 5, "seller-1", false);

        mockMvc.perform(post("/api/v1/inventory/p-1/restock")
                        .header("Authorization", "Bearer " + sellerToken())
                        .contentType(APPLICATION_JSON)
                        .content("{\"quantity\":5}"))
                .andExpect(status().isForbidden());
    }
}
