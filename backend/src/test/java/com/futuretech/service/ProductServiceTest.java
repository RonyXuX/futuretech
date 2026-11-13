package com.futuretech.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.futuretech.dto.PageResponse;
import com.futuretech.dto.ProductDetailDto;
import com.futuretech.dto.ProductFilter;
import com.futuretech.dto.ProductSort;
import com.futuretech.dto.ProductSpecsDto;
import com.futuretech.dto.ProductSummaryDto;
import com.futuretech.entity.Category;
import com.futuretech.entity.Product;
import com.futuretech.entity.enums.ProductType;
import com.futuretech.exception.ResourceNotFoundException;
import com.futuretech.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductSpecsService productSpecsService;

    @InjectMocks
    private ProductService productService;

    private static final ProductFilter NO_FILTER = new ProductFilter(null, null, null, null, null, null, null);

    @Test
    void getProduct_unknownId_throwsNotFound() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProduct(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Product 999 not found");
    }

    @Test
    void getProduct_inactiveProduct_isTreatedAsNotFound() {
        Product hidden = cpu(1L);
        hidden.setActive(false);
        when(productRepository.findById(1L)).thenReturn(Optional.of(hidden));

        assertThatThrownBy(() -> productService.getProduct(1L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getProduct_returnsProductWithItsSpecs() {
        Product cpu = cpu(1L);
        ProductSpecsDto.Cpu specs = new ProductSpecsDto.Cpu("AM5", 8, 16, 4.7, 5.2, 120, true);
        when(productRepository.findById(1L)).thenReturn(Optional.of(cpu));
        when(productSpecsService.findSpecs(cpu)).thenReturn(specs);

        ProductDetailDto result = productService.getProduct(1L);

        assertThat(result.name()).isEqualTo("AMD Ryzen 7 9800X3D");
        assertThat(result.category().slug()).isEqualTo("processors");
        assertThat(result.inStock()).isTrue();
        assertThat(result.specs()).isEqualTo(specs);
    }

    @Test
    void searchProducts_correctsBadPagingValues() {
        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenAnswer(inv -> new PageImpl<>(List.of(cpu(1L)), inv.getArgument(1), 1));

        PageResponse<ProductSummaryDto> result = productService.searchProducts(NO_FILTER, ProductSort.PRICE_ASC, -3, 500);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).findAll(any(Specification.class), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isZero();                       // -3 -> 0
        assertThat(pageable.getValue().getPageSize()).isEqualTo(ProductService.MAX_PAGE_SIZE); // 500 -> 100
        assertThat(pageable.getValue().getSort().getOrderFor("price").isAscending()).isTrue();

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().categoryName()).isEqualTo("Processors");
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void getBrands_withoutCategory_returnsAllBrands() {
        when(productRepository.findActiveBrands()).thenReturn(List.of("AMD", "Intel"));

        assertThat(productService.getBrands(null)).containsExactly("AMD", "Intel");
    }

    @Test
    void getBrands_withCategory_returnsOnlyThatCategory() {
        when(productRepository.findActiveBrandsInCategory("cases")).thenReturn(List.of("NZXT"));

        assertThat(productService.getBrands("cases")).containsExactly("NZXT");
    }

    private static Product cpu(Long id) {
        Category category = new Category();
        category.setId(1L);
        category.setName("Processors");
        category.setSlug("processors");

        Product p = new Product();
        p.setId(id);
        p.setSku("FT-CPU-003");
        p.setName("AMD Ryzen 7 9800X3D");
        p.setBrand("AMD");
        p.setPrice(new BigDecimal("451.99"));
        p.setStockQuantity(10);
        p.setCategory(category);
        p.setProductType(ProductType.CPU);
        p.setPerformanceScore(85);
        return p;
    }
}
