package com.futuretech.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.futuretech.config.SecurityConfig;
import com.futuretech.dto.PageResponse;
import com.futuretech.dto.admin.AdminOrderDto;
import com.futuretech.dto.admin.OrderCustomerDto;
import com.futuretech.dto.order.OrderDto;
import com.futuretech.dto.order.PlaceOrderRequest;
import com.futuretech.entity.enums.OrderStatus;
import com.futuretech.service.AdminOrderService;
import com.futuretech.support.TestSecurity;

@WebMvcTest(AdminOrderController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = TestSecurity.JWT_SECRET_PROPERTY)
class AdminOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminOrderService adminOrderService;

    // ---------- who may call it ----------

    @Test
    void withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/orders")).andExpect(status().isUnauthorized());
    }

    @Test
    void asCustomer_returns403() throws Exception {
        mockMvc.perform(patch("/api/admin/orders/50/status").with(customer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\": \"SHIPPED\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(adminOrderService);
    }

    // ---------- endpoints ----------

    @Test
    void list_withoutStatus_passesNull() throws Exception {
        when(adminOrderService.searchOrders(any(), anyInt(), anyInt()))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/admin/orders").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        verify(adminOrderService).searchOrders(isNull(), eq(0), eq(20));
    }

    @Test
    void list_withStatus_passesIt() throws Exception {
        when(adminOrderService.searchOrders(any(), anyInt(), anyInt()))
                .thenReturn(new PageResponse<>(List.of(), 1, 10, 0, 0));

        mockMvc.perform(get("/api/admin/orders").param("status", "SHIPPED").param("page", "1")
                        .param("size", "10").with(admin()))
                .andExpect(status().isOk());

        verify(adminOrderService).searchOrders(OrderStatus.SHIPPED, 1, 10);
    }

    @Test
    void list_unknownStatus_returns400() throws Exception {
        mockMvc.perform(get("/api/admin/orders").param("status", "LOST").with(admin()))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(adminOrderService);
    }

    @Test
    void getOrder_returnsOrderAndCustomer() throws Exception {
        when(adminOrderService.getOrder(50L)).thenReturn(detail(OrderStatus.PLACED));

        mockMvc.perform(get("/api/admin/orders/50").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.order.id").value(50))
                .andExpect(jsonPath("$.customer.email").value("lina.haddad@example.com"))
                .andExpect(jsonPath("$.customer.passwordHash").doesNotExist());
    }

    @Test
    void changeStatus_passesTheNewStatus() throws Exception {
        when(adminOrderService.changeStatus(50L, OrderStatus.SHIPPED)).thenReturn(detail(OrderStatus.SHIPPED));

        mockMvc.perform(patch("/api/admin/orders/50/status").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\": \"SHIPPED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.order.status").value("SHIPPED"));
    }

    @Test
    void changeStatus_missingStatus_returns400() throws Exception {
        mockMvc.perform(patch("/api/admin/orders/50/status").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.status").exists());
        verifyNoInteractions(adminOrderService);
    }

    // ---------- helpers ----------

    private static RequestPostProcessor admin() {
        return jwt().jwt(j -> j.subject("1")).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private static RequestPostProcessor customer() {
        return jwt().jwt(j -> j.subject("5")).authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
    }

    private static AdminOrderDto detail(OrderStatus status) {
        OrderDto order = new OrderDto(50L, status, new BigDecimal("451.99"), 451, LocalDateTime.of(2026, 11, 13, 10, 0),
                new PlaceOrderRequest.Shipping("Lina Haddad", "12 Hamra Street", "Beirut", "+961 70 000 001"),
                List.of());
        return new AdminOrderDto(order, new OrderCustomerDto(5L, "lina.haddad@example.com", "Lina", "Haddad"));
    }
}
