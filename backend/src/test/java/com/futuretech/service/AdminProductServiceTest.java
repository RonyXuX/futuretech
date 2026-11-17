package com.futuretech.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.futuretech.dto.ProductDetailDto;
import com.futuretech.dto.admin.ProductRequest;
import com.futuretech.dto.admin.SpecsRequest;
import com.futuretech.entity.Category;
import com.futuretech.entity.Product;
import com.futuretech.entity.enums.CoolerType;
import com.futuretech.entity.enums.ProductType;
import com.futuretech.exception.ConflictException;
import com.futuretech.exception.InvalidInputException;
import com.futuretech.exception.ResourceNotFoundException;
import com.futuretech.repository.CategoryRepository;
import com.futuretech.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class AdminProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductSpecsService productSpecsService;

    @InjectMocks
    private AdminProductService service;

    private static final SpecsRequest.Cpu CPU_SPECS =
            new SpecsRequest.Cpu("AM5", 8, 16, 4.7, 5.2, 120, true);

    // ---------- create ----------

    @Test
    void create_savesProductThenItsSpecs() {
        when(categoryRepository.findBySlug("processors")).thenReturn(Optional.of(category()));
        when(productRepository.findBySku("FT-CPU-099")).thenReturn(Optional.empty());
        when(productRepository.saveAndFlush(any(Product.class))).thenAnswer(call -> {
            Product saved = call.getArgument(0);
            saved.setId(42L);  // what the database would do
            return saved;
        });

        ProductDetailDto result = service.createProduct(request(ProductType.CPU, CPU_SPECS));

        assertThat(result.id()).isEqualTo(42L);
        assertThat(result.sku()).isEqualTo("FT-CPU-099");
        assertThat(result.name()).isEqualTo("AMD Ryzen 7 9700X");  // trimmed
        assertThat(result.active()).isTrue();
        verify(productSpecsService).saveSpecs(any(Product.class), any(SpecsRequest.Cpu.class));
    }

    @Test
    void create_accessoryWithoutSpecs_savesNoSpecs() {
        when(categoryRepository.findBySlug("accessories")).thenReturn(Optional.of(category()));
        when(productRepository.findBySku("FT-CPU-099")).thenReturn(Optional.empty());

        service.createProduct(request(ProductType.ACCESSORY, null));

        verify(productRepository).saveAndFlush(any(Product.class));
        verify(productSpecsService, never()).saveSpecs(any(), any());
    }

    @Test
    void create_duplicateSku_throwsConflictAndSavesNothing() {
        when(categoryRepository.findBySlug("processors")).thenReturn(Optional.of(category()));
        when(productRepository.findBySku("FT-CPU-099")).thenReturn(Optional.of(product(7L, ProductType.CPU)));

        ProductRequest productRequest = request(ProductType.CPU, CPU_SPECS);
        assertThatThrownBy(() -> service.createProduct(productRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("FT-CPU-099");
        verify(productRepository, never()).saveAndFlush(any());
    }

    @Test
    void create_sameSkuAtTheSameMoment_givesTheNormal409() {
        // both admins passed findBySku; the database's UNIQUE constraint refuses the second one
        when(categoryRepository.findBySlug("processors")).thenReturn(Optional.of(category()));
        when(productRepository.findBySku("FT-CPU-099")).thenReturn(Optional.empty());
        when(productRepository.saveAndFlush(any(Product.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint"));

        ProductRequest productRequest = request(ProductType.CPU, CPU_SPECS);
        assertThatThrownBy(() -> service.createProduct(productRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("FT-CPU-099");
        verify(productSpecsService, never()).saveSpecs(any(), any());
    }

    @Test
    void create_unknownCategory_failsOnThatField() {
        when(categoryRepository.findBySlug("processors")).thenReturn(Optional.empty());

        ProductRequest productRequest = request(ProductType.CPU, CPU_SPECS);
        assertThatThrownBy(() -> service.createProduct(productRequest))
                .isInstanceOf(InvalidInputException.class)
                .extracting("field").isEqualTo("categorySlug");
    }

    // ---------- category checks ----------

    @Test
    void create_cpuInTheLaptopsCategory_isRejected() {
        ProductRequest productRequest = request(ProductType.CPU, CPU_SPECS, "laptops");
        assertThatThrownBy(() -> service.createProduct(productRequest))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("processors")
                .extracting("field").isEqualTo("categorySlug");
        verify(productRepository, never()).saveAndFlush(any());
    }

    @Test
    void update_movingToTheWrongCategory_isRejected() {
        when(productRepository.findById(7L)).thenReturn(Optional.of(product(7L, ProductType.CPU)));

        ProductRequest productRequest = request(ProductType.CPU, CPU_SPECS, "graphics-cards");
        assertThatThrownBy(() -> service.updateProduct(7L, productRequest))
                .isInstanceOf(InvalidInputException.class)
                .extracting("field").isEqualTo("categorySlug");
        verify(productSpecsService, never()).saveSpecs(any(), any());
    }

    @Test
    void everyProductType_hasItsOwnCategory() {
        long distinctSlugs = Arrays.stream(ProductType.values()).map(ProductType::categorySlug).distinct().count();
        assertThat(distinctSlugs).isEqualTo(ProductType.values().length);
    }

    // ---------- spec checks (all fail before anything is saved) ----------

    @Test
    void create_cpuWithoutSpecs_isRejected() {
        assertSpecsError(request(ProductType.CPU, null), "specs");
    }

    @Test
    void create_specsOfTheWrongKind_areRejected() {
        SpecsRequest.Gpu gpuSpecs = new SpecsRequest.Gpu("GeForce RTX 5070", 12, 300, 250);
        assertSpecsError(request(ProductType.CPU, gpuSpecs), "specs.kind");
    }

    @Test
    void create_accessoryWithSpecs_isRejected() {
        assertSpecsError(request(ProductType.ACCESSORY, CPU_SPECS), "specs");
    }

    @Test
    void computerSpecs_fitLaptopsAndDesktopsOnly() {
        SpecsRequest.Computer computer = new SpecsRequest.Computer("Core Ultra 7", "RTX 5060", 16, 1000, 15.6, "Windows 11");
        assertThat(computer.fits(ProductType.LAPTOP)).isTrue();
        assertThat(computer.fits(ProductType.DESKTOP)).isTrue();
        assertThat(computer.fits(ProductType.CPU)).isFalse();
    }

    @Test
    void create_cpuWithFewerThreadsThanCores_isRejected() {
        SpecsRequest.Cpu bad = new SpecsRequest.Cpu("AM5", 8, 4, 4.7, 5.2, 120, true);
        assertSpecsError(request(ProductType.CPU, bad), "specs.threads");
    }

    @Test
    void create_cpuWithBoostBelowBase_isRejected() {
        SpecsRequest.Cpu bad = new SpecsRequest.Cpu("AM5", 8, 16, 4.7, 3.0, 120, true);
        assertSpecsError(request(ProductType.CPU, bad), "specs.boostClockGhz");
    }

    @Test
    void create_airCoolerWithoutHeight_isRejected() {
        SpecsRequest.Cooler bad = new SpecsRequest.Cooler(CoolerType.AIR, null, null, List.of("AM5"));
        assertSpecsError(request(ProductType.COOLER, bad), "specs.heightMm");
    }

    @Test
    void create_airCoolerWithRadiator_isRejected() {
        SpecsRequest.Cooler bad = new SpecsRequest.Cooler(CoolerType.AIR, 155, 240, List.of("AM5"));
        assertSpecsError(request(ProductType.COOLER, bad), "specs.radiatorMm");
    }

    @Test
    void create_liquidCoolerWithoutRadiator_isRejected() {
        SpecsRequest.Cooler bad = new SpecsRequest.Cooler(CoolerType.LIQUID, null, null, List.of("AM5"));
        assertSpecsError(request(ProductType.COOLER, bad), "specs.radiatorMm");
    }

    // ---------- update ----------

    @Test
    void update_changesFieldsAndSpecs() {
        Product existing = product(7L, ProductType.CPU);
        when(productRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(categoryRepository.findBySlug("processors")).thenReturn(Optional.of(category()));
        when(productRepository.findBySku("FT-CPU-099")).thenReturn(Optional.empty());

        ProductDetailDto result = service.updateProduct(7L, request(ProductType.CPU, CPU_SPECS));

        assertThat(result.price()).isEqualByComparingTo("359.99");
        assertThat(existing.getSku()).isEqualTo("FT-CPU-099");
        verify(productSpecsService).saveSpecs(existing, CPU_SPECS);
    }

    @Test
    void update_keepingItsOwnSku_isNotAConflict() {
        Product existing = product(7L, ProductType.CPU);
        when(productRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(categoryRepository.findBySlug("processors")).thenReturn(Optional.of(category()));
        when(productRepository.findBySku("FT-CPU-099")).thenReturn(Optional.of(existing));

        service.updateProduct(7L, request(ProductType.CPU, CPU_SPECS));

        verify(productSpecsService).saveSpecs(existing, CPU_SPECS);
    }

    @Test
    void update_skuOfAnotherProduct_throwsConflict() {
        when(productRepository.findById(7L)).thenReturn(Optional.of(product(7L, ProductType.CPU)));
        when(categoryRepository.findBySlug("processors")).thenReturn(Optional.of(category()));
        when(productRepository.findBySku("FT-CPU-099")).thenReturn(Optional.of(product(8L, ProductType.CPU)));

        ProductRequest productRequest = request(ProductType.CPU, CPU_SPECS);
        assertThatThrownBy(() -> service.updateProduct(7L, productRequest))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void update_changingTheProductType_isRejected() {
        when(productRepository.findById(7L)).thenReturn(Optional.of(product(7L, ProductType.CPU)));
        SpecsRequest.Gpu gpuSpecs = new SpecsRequest.Gpu("GeForce RTX 5070", 12, 300, 250);

        ProductRequest productRequest = request(ProductType.GPU, gpuSpecs);
        assertThatThrownBy(() -> service.updateProduct(7L, productRequest))
                .isInstanceOf(InvalidInputException.class)
                .extracting("field").isEqualTo("productType");
        verify(productSpecsService, never()).saveSpecs(any(), any());
    }

    @Test
    void update_unknownProduct_throwsNotFound() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        ProductRequest productRequest = request(ProductType.CPU, CPU_SPECS);
        assertThatThrownBy(() -> service.updateProduct(999L, productRequest))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- hide / show ----------

    @Test
    void setActive_hidesAndShowsAProduct() {
        Product existing = product(7L, ProductType.CPU);
        when(productRepository.findById(7L)).thenReturn(Optional.of(existing));

        assertThat(service.setActive(7L, false).active()).isFalse();
        assertThat(existing.getActive()).isFalse();

        assertThat(service.setActive(7L, true).active()).isTrue();
    }

    @Test
    void getProduct_alsoReturnsHiddenProducts() {
        Product hidden = product(7L, ProductType.CPU);
        hidden.setActive(false);
        when(productRepository.findById(7L)).thenReturn(Optional.of(hidden));

        assertThat(service.getProduct(7L).active()).isFalse();
    }

    // ---------- helpers ----------

    private void assertSpecsError(ProductRequest request, String field) {
        assertThatThrownBy(() -> service.createProduct(request))
                .isInstanceOf(InvalidInputException.class)
                .extracting("field").isEqualTo(field);
        verify(productRepository, never()).saveAndFlush(any());
    }

    private static ProductRequest request(ProductType type, SpecsRequest specs) {
        return request(type, specs, type.categorySlug());
    }

    private static ProductRequest request(ProductType type, SpecsRequest specs, String categorySlug) {
        return new ProductRequest(" FT-CPU-099 ", "  AMD Ryzen 7 9700X ", "AMD", "8 cores, 16 threads.", "  ",
                new BigDecimal("359.99"), 15, categorySlug, type, 36, 74, specs);
    }

    private static Category category() {
        Category category = new Category();
        category.setId(1L);
        category.setName("Processors");
        category.setSlug("processors");
        return category;
    }

    private static Product product(Long id, ProductType type) {
        Product p = new Product();
        p.setId(id);
        p.setSku("FT-CPU-00" + id);
        p.setName("Old name");
        p.setBrand("AMD");
        p.setPrice(new BigDecimal("299.99"));
        p.setStockQuantity(3);
        p.setCategory(category());
        p.setProductType(type);
        return p;
    }
}
