package com.futuretech.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.futuretech.dto.PageResponse;
import com.futuretech.dto.ProductDetailDto;
import com.futuretech.dto.ProductFilter;
import com.futuretech.dto.ProductSort;
import com.futuretech.dto.ProductSummaryDto;
import com.futuretech.entity.enums.ProductType;
import com.futuretech.service.ProductService;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public PageResponse<ProductSummaryDto> searchProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) ProductType type,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(defaultValue = "NEWEST") ProductSort sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        ProductFilter filter = new ProductFilter(search, category, type, brand, minPrice, maxPrice, inStock);
        return productService.searchProducts(filter, sort, page, size);
    }

    @GetMapping("/brands")
    public List<String> getBrands(@RequestParam(required = false) String category) {
        return productService.getBrands(category);
    }

    @GetMapping("/{id}")
    public ProductDetailDto getProduct(@PathVariable Long id) {
        return productService.getProduct(id);
    }
}
