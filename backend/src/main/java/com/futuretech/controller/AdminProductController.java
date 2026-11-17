package com.futuretech.controller;

import java.math.BigDecimal;
import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.futuretech.dto.PageResponse;
import com.futuretech.dto.ProductDetailDto;
import com.futuretech.dto.ProductFilter;
import com.futuretech.dto.ProductSort;
import com.futuretech.dto.ProductSummaryDto;
import com.futuretech.dto.admin.ActiveRequest;
import com.futuretech.dto.admin.ProductRequest;
import com.futuretech.entity.enums.ProductType;
import com.futuretech.service.AdminProductService;

import jakarta.validation.Valid;

// Only admins get here: SecurityConfig requires ROLE_ADMIN for /api/admin/**
@RestController
@RequestMapping("/api/admin/products")
public class AdminProductController {

    private final AdminProductService adminProductService;

    public AdminProductController(AdminProductService adminProductService) {
        this.adminProductService = adminProductService;
    }

    /** Same filters as the store; active=true/false shows only visible/hidden products. */
    @GetMapping
    public PageResponse<ProductSummaryDto> searchProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) ProductType type,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "NEWEST") ProductSort sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        ProductFilter filter = new ProductFilter(search, category, type, brand, minPrice, maxPrice, inStock);
        return adminProductService.searchProducts(filter, active, sort, page, size);
    }

    @GetMapping("/{id}")
    public ProductDetailDto getProduct(@PathVariable Long id) {
        return adminProductService.getProduct(id);
    }

    @PostMapping
    public ResponseEntity<ProductDetailDto> createProduct(@Valid @RequestBody ProductRequest request) {
        ProductDetailDto product = adminProductService.createProduct(request);
        return ResponseEntity.created(URI.create("/api/admin/products/" + product.id())).body(product);
    }

    @PutMapping("/{id}")
    public ProductDetailDto updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return adminProductService.updateProduct(id, request);
    }

    @PatchMapping("/{id}/active")
    public ProductDetailDto setActive(@PathVariable Long id, @Valid @RequestBody ActiveRequest request) {
        return adminProductService.setActive(id, request.active());
    }
}
