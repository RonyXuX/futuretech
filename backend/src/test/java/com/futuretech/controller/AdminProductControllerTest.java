package com.futuretech.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import com.futuretech.dto.CategoryDto;
import com.futuretech.dto.PageResponse;
import com.futuretech.dto.ProductDetailDto;
import com.futuretech.dto.ProductSpecsDto;
import com.futuretech.dto.admin.ProductRequest;
import com.futuretech.dto.admin.SpecsRequest;
import com.futuretech.entity.enums.ProductType;
import com.futuretech.service.AdminProductService;
import com.futuretech.support.TestSecurity;

@WebMvcTest(AdminProductController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = TestSecurity.JWT_SECRET_PROPERTY)
class AdminProductControllerTest {

    private static final String CPU_JSON = """
            {
              "sku": "FT-CPU-099", "name": "AMD Ryzen 7 9700X", "brand": "AMD",
              "price": 359.99, "stockQuantity": 15, "categorySlug": "processors",
              "productType": "CPU", "warrantyMonths": 36, "performanceScore": 74,
              "specs": {
                "kind": "CPU", "socket": "AM5", "cores": 8, "threads": 16,
                "baseClockGhz": 3.8, "boostClockGhz": 5.5, "tdpWatts": 65, "integratedGraphics": true
              }
            }""";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminProductService adminProductService;

    // ---------- who may call it ----------

    @Test
    void withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/products")).andExpect(status().isUnauthorized());
    }

    @Test
    void asCustomer_returns403() throws Exception {
        mockMvc.perform(post("/api/admin/products").with(customer())
                        .contentType(MediaType.APPLICATION_JSON).content(CPU_JSON))
                .andExpect(status().isForbidden());
        verifyNoInteractions(adminProductService);
    }

    // ---------- endpoints ----------

    @Test
    void list_passesActiveFilterToService() throws Exception {
        when(adminProductService.searchProducts(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/admin/products").param("active", "false").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        verify(adminProductService).searchProducts(any(), eq(false), any(), eq(0), eq(20));
    }

    @Test
    void create_readsTypedSpecsAndReturns201WithLocation() throws Exception {
        when(adminProductService.createProduct(any())).thenReturn(detail());

        mockMvc.perform(post("/api/admin/products").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content(CPU_JSON))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/admin/products/42"))
                .andExpect(jsonPath("$.specs.socket").value("AM5"))
                .andExpect(jsonPath("$.active").value(true));

        // "kind": "CPU" in the JSON must become a SpecsRequest.Cpu
        ArgumentCaptor<ProductRequest> request = ArgumentCaptor.forClass(ProductRequest.class);
        verify(adminProductService).createProduct(request.capture());
        assertThat(request.getValue().specs()).isInstanceOf(SpecsRequest.Cpu.class);
        assertThat(((SpecsRequest.Cpu) request.getValue().specs()).threads()).isEqualTo(16);
    }

    @Test
    void create_unknownSpecsKind_returns400() throws Exception {
        String json = CPU_JSON.replace("\"kind\": \"CPU\"", "\"kind\": \"TOASTER\"");

        mockMvc.perform(post("/api/admin/products").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest());
        verify(adminProductService, never()).createProduct(any());
    }

    @Test
    void create_invalidFields_returns400WithEachField() throws Exception {
        String json = CPU_JSON
                .replace("\"price\": 359.99", "\"price\": -1")
                .replace("\"cores\": 8", "\"cores\": 0");

        mockMvc.perform(post("/api/admin/products").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.price").exists())
                .andExpect(jsonPath("$.errors['specs.cores']").exists());
        verify(adminProductService, never()).createProduct(any());
    }

    @Test
    void setActive_hidesProduct() throws Exception {
        when(adminProductService.setActive(42L, false)).thenReturn(detail());

        mockMvc.perform(patch("/api/admin/products/42/active").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"active\": false}"))
                .andExpect(status().isOk());

        verify(adminProductService).setActive(42L, false);
    }

    @Test
    void setActive_missingValue_returns400() throws Exception {
        mockMvc.perform(patch("/api/admin/products/42/active").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ---------- helpers ----------

    private static RequestPostProcessor admin() {
        return jwt().jwt(j -> j.subject("1")).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private static RequestPostProcessor customer() {
        return jwt().jwt(j -> j.subject("5")).authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
    }

    private static ProductDetailDto detail() {
        return new ProductDetailDto(42L, "FT-CPU-099", "AMD Ryzen 7 9700X", "AMD", null,
                new BigDecimal("359.99"), true, 15, ProductType.CPU,
                new CategoryDto(1L, "Processors", "processors", null), 36, 74, null, true,
                new ProductSpecsDto.Cpu("AM5", 8, 16, 3.8, 5.5, 65, true));
    }
}
