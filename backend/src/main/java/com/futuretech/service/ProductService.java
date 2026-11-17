package com.futuretech.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.futuretech.dto.PageResponse;
import com.futuretech.dto.ProductDetailDto;
import com.futuretech.dto.ProductFilter;
import com.futuretech.dto.ProductSort;
import com.futuretech.dto.ProductSummaryDto;
import com.futuretech.entity.Product;
import com.futuretech.exception.ResourceNotFoundException;
import com.futuretech.repository.ProductRepository;
import com.futuretech.repository.ProductSpecifications;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductSpecsService productSpecsService;

    public ProductService(ProductRepository productRepository, ProductSpecsService productSpecsService) {
        this.productRepository = productRepository;
        this.productSpecsService = productSpecsService;
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductSummaryDto> searchProducts(ProductFilter filter, ProductSort sort, int page, int size) {
        return PageResponse.from(
                productRepository.findAll(ProductSpecifications.fromFilter(filter), Paging.of(page, size, sort.toSort())),
                ProductSummaryDto::from);
    }

    /** Hidden (inactive) products count as not found. */
    @Transactional(readOnly = true)
    public ProductDetailDto getProduct(Long id) {
        Product product = productRepository.findById(id)
                .filter(Product::getActive)
                .orElseThrow(() -> new ResourceNotFoundException("Product " + id + " not found"));
        return ProductDetailDto.from(product, productSpecsService.findSpecs(product));
    }

    @Transactional(readOnly = true)
    public List<String> getBrands(String categorySlug) {
        if (categorySlug == null || categorySlug.isBlank()) {
            return productRepository.findActiveBrands();
        }
        return productRepository.findActiveBrandsInCategory(categorySlug);
    }
}
