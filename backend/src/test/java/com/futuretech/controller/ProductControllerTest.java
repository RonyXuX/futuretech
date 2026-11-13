package com.futuretech.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.futuretech.config.SecurityConfig;
import com.futuretech.support.TestSecurity;
import com.futuretech.dto.CategoryDto;
import com.futuretech.dto.PageResponse;
import com.futuretech.dto.ProductDetailDto;
import com.futuretech.dto.ProductFilter;
import com.futuretech.dto.ProductSort;
import com.futuretech.dto.ProductSpecsDto;
import com.futuretech.dto.ProductSummaryDto;
import com.futuretech.entity.enums.ProductType;
import com.futuretech.exception.ResourceNotFoundException;
import com.futuretech.service.ProductService;

/**
 * Web-layer tests: starts only the controller, the error handler and the security/CORS config
 * (no database), and sends fake HTTP requests with MockMvc. The service is a Mockito fake.
 */
@WebMvcTest(ProductController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = TestSecurity.JWT_SECRET_PROPERTY)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @Test
    void searchProducts_passesFiltersToServiceAndReturnsPage() throws Exception {
        when(productService.searchProducts(any(), any(), anyInt(), anyInt()))
                .thenReturn(new PageResponse<>(List.of(summary()), 0, 20, 1, 1));

        mockMvc.perform(get("/api/products")
                        .param("search", "rtx")
                        .param("type", "GPU")
                        .param("maxPrice", "600")
                        .param("sort", "PRICE_ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("NVIDIA GeForce RTX 5060 Ti 16GB"))
                .andExpect(jsonPath("$.content[0].inStock").value(true))
                .andExpect(jsonPath("$.totalElements").value(1));

        ArgumentCaptor<ProductFilter> filter = ArgumentCaptor.forClass(ProductFilter.class);
        verify(productService).searchProducts(filter.capture(), eq(ProductSort.PRICE_ASC), eq(0), eq(20));
        assertThat(filter.getValue().search()).isEqualTo("rtx");
        assertThat(filter.getValue().type()).isEqualTo(ProductType.GPU);
        assertThat(filter.getValue().maxPrice()).isEqualByComparingTo("600");
        assertThat(filter.getValue().categorySlug()).isNull();
    }

    @Test
    void searchProducts_unknownSortValue_returns400() throws Exception {
        mockMvc.perform(get("/api/products").param("sort", "CHEAPEST"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getProduct_returnsSpecsForTheProductType() throws Exception {
        when(productService.getProduct(7L)).thenReturn(detail());

        mockMvc.perform(get("/api/products/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category.slug").value("graphics-cards"))
                .andExpect(jsonPath("$.specs.lengthMm").value(280))
                .andExpect(jsonPath("$.specs.tdpWatts").value(180));
    }

    @Test
    void getProduct_unknownId_returns404ProblemDetail() throws Exception {
        when(productService.getProduct(999L)).thenThrow(new ResourceNotFoundException("Product 999 not found"));

        mockMvc.perform(get("/api/products/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Product 999 not found"));
    }

    @Test
    void getProduct_idIsNotANumber_returns400() throws Exception {
        mockMvc.perform(get("/api/products/abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getBrands_returnsList() throws Exception {
        when(productService.getBrands("cases")).thenReturn(List.of("Corsair", "NZXT"));

        mockMvc.perform(get("/api/products/brands").param("category", "cases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1]").value("NZXT"));
    }

    @Test
    void cors_allowsTheAngularDevServer() throws Exception {
        mockMvc.perform(options("/api/products")
                        .header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
    }

    @Test
    void cors_blocksOtherWebsites() throws Exception {
        mockMvc.perform(options("/api/products")
                        .header("Origin", "http://evil.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }

    private static ProductSummaryDto summary() {
        return new ProductSummaryDto(7L, "FT-GPU-001", "NVIDIA GeForce RTX 5060 Ti 16GB", "NVIDIA",
                new BigDecimal("499.99"), true, 12, ProductType.GPU, "graphics-cards", "Graphics Cards", 62, null);
    }

    private static ProductDetailDto detail() {
        return new ProductDetailDto(7L, "FT-GPU-001", "NVIDIA GeForce RTX 5060 Ti 16GB", "NVIDIA",
                "GeForce RTX 5060 Ti, 16 GB VRAM.", new BigDecimal("499.99"), true, 12, ProductType.GPU,
                new CategoryDto(4L, "Graphics Cards", "graphics-cards", null), 36, 62, null,
                new ProductSpecsDto.Gpu("GeForce RTX 5060 Ti", 16, 280, 180));
    }
}
