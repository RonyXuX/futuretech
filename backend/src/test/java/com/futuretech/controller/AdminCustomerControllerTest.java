package com.futuretech.controller;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.futuretech.dto.admin.AdminCustomerDto;
import com.futuretech.service.AdminCustomerService;
import com.futuretech.support.TestSecurity;

@WebMvcTest(AdminCustomerController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = TestSecurity.JWT_SECRET_PROPERTY)
class AdminCustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminCustomerService adminCustomerService;

    // ---------- who may call it ----------

    @Test
    void withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/customers")).andExpect(status().isUnauthorized());
    }

    @Test
    void asCustomer_cantGiveThemselvesPoints() throws Exception {
        mockMvc.perform(post("/api/admin/customers/5/points").with(customer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\": 1000}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(adminCustomerService);
    }

    // ---------- endpoints ----------

    @Test
    void list_withoutSearch_passesNull() throws Exception {
        when(adminCustomerService.searchCustomers(isNull(), anyInt(), anyInt()))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/admin/customers").with(admin()))
                .andExpect(status().isOk());

        verify(adminCustomerService).searchCustomers(isNull(), eq(0), eq(20));
    }

    @Test
    void list_passesSearchText() throws Exception {
        when(adminCustomerService.searchCustomers(anyString(), anyInt(), anyInt()))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/admin/customers").param("search", "haddad").with(admin()))
                .andExpect(status().isOk());

        verify(adminCustomerService).searchCustomers("haddad", 0, 20);
    }

    @Test
    void getCustomer_returnsProfileWithoutSecrets() throws Exception {
        when(adminCustomerService.getCustomer(5L)).thenReturn(customerDto(120));

        mockMvc.perform(get("/api/admin/customers/5").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("lina.haddad@example.com"))
                .andExpect(jsonPath("$.pointsBalance").value(120))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void adjustPoints_passesTheAmount() throws Exception {
        when(adminCustomerService.adjustPoints(5L, -20)).thenReturn(customerDto(100));

        mockMvc.perform(post("/api/admin/customers/5/points").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\": -20}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pointsBalance").value(100));
    }

    @Test
    void adjustPoints_missingAmount_returns400() throws Exception {
        mockMvc.perform(post("/api/admin/customers/5/points").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.amount").exists());
        verifyNoInteractions(adminCustomerService);
    }

    @Test
    void adjustPoints_hugeAmount_returns400() throws Exception {
        mockMvc.perform(post("/api/admin/customers/5/points").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\": 1000000}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.amount").exists());
        verifyNoInteractions(adminCustomerService);
    }

    // ---------- helpers ----------

    private static RequestPostProcessor admin() {
        return jwt().jwt(j -> j.subject("1")).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private static RequestPostProcessor customer() {
        return jwt().jwt(j -> j.subject("5")).authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
    }

    private static AdminCustomerDto customerDto(int points) {
        return new AdminCustomerDto(5L, "lina.haddad@example.com", "Lina", "Haddad", null, points,
                LocalDateTime.of(2026, 11, 1, 9, 0), List.of(), List.of());
    }
}
