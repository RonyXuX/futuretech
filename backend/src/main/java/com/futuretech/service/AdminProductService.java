package com.futuretech.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.futuretech.dto.PageResponse;
import com.futuretech.dto.ProductDetailDto;
import com.futuretech.dto.ProductFilter;
import com.futuretech.dto.ProductSort;
import com.futuretech.dto.ProductSummaryDto;
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
import com.futuretech.repository.ProductSpecifications;

/**
 * Admin product management. Unlike the store, admins also see hidden products.
 * Products are never deleted (old orders point to them); they are hidden with setActive(false).
 */
@Service
public class AdminProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductSpecsService productSpecsService;

    public AdminProductService(ProductRepository productRepository, CategoryRepository categoryRepository,
                               ProductSpecsService productSpecsService) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.productSpecsService = productSpecsService;
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductSummaryDto> searchProducts(ProductFilter filter, Boolean active, ProductSort sort,
                                                          int page, int size) {
        return PageResponse.from(
                productRepository.findAll(ProductSpecifications.forAdmin(filter, active),
                        ProductService.pageRequest(sort, page, size)),
                ProductSummaryDto::from);
    }

    @Transactional(readOnly = true)
    public ProductDetailDto getProduct(Long id) {
        return toDto(findProduct(id));
    }

    @Transactional
    public ProductDetailDto createProduct(ProductRequest request) {
        checkSpecs(request.productType(), request.specs());
        checkCategory(request.productType(), request.categorySlug());
        Category category = findCategory(request.categorySlug());
        String sku = request.sku().trim();
        if (productRepository.findBySku(sku).isPresent()) {
            throw skuTaken(sku);
        }

        Product product = new Product();
        product.setProductType(request.productType());
        copyFields(request, product, sku, category);
        productRepository.save(product);  // gives the product its id, which the spec row reuses
        if (request.specs() != null) {
            productSpecsService.saveSpecs(product, request.specs());
        }
        return toDto(product);
    }

    /** Replaces every field. The product type can't change: the old spec row would no longer match. */
    @Transactional
    public ProductDetailDto updateProduct(Long id, ProductRequest request) {
        Product product = findProduct(id);
        if (request.productType() != product.getProductType()) {
            throw new InvalidInputException("productType", "The product type can't be changed (it is "
                    + product.getProductType() + "). Create a new product instead.");
        }
        checkSpecs(product.getProductType(), request.specs());
        checkCategory(product.getProductType(), request.categorySlug());
        Category category = findCategory(request.categorySlug());
        String sku = request.sku().trim();
        productRepository.findBySku(sku)
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw skuTaken(sku);
                });

        copyFields(request, product, sku, category);
        if (request.specs() != null) {
            productSpecsService.saveSpecs(product, request.specs());
        }
        return toDto(product);
    }

    @Transactional
    public ProductDetailDto setActive(Long id, boolean active) {
        Product product = findProduct(id);
        product.setActive(active);
        return toDto(product);
    }

    /**
     * PC parts and computers need specs (the compatibility rules and the AI read them);
     * accessories and peripherals have no spec table, so they must not send any.
     */
    private static void checkSpecs(ProductType type, SpecsRequest specs) {
        boolean needsSpecs = type != ProductType.ACCESSORY && type != ProductType.PERIPHERAL;
        if (!needsSpecs) {
            if (specs != null) {
                throw new InvalidInputException("specs", type + " products have no specs; leave this empty.");
            }
            return;
        }
        if (specs == null) {
            throw new InvalidInputException("specs", type + " products need specs.");
        }
        if (!specs.fits(type)) {
            throw new InvalidInputException("specs.kind", "These specs don't belong to a " + type + " product.");
        }

        // Rules between two fields, which the annotations on SpecsRequest can't express
        switch (specs) {
            case SpecsRequest.Cpu cpu -> {
                if (cpu.threads() < cpu.cores()) {
                    throw new InvalidInputException("specs.threads", "must be at least the number of cores");
                }
                if (cpu.boostClockGhz() != null && cpu.boostClockGhz() < cpu.baseClockGhz()) {
                    throw new InvalidInputException("specs.boostClockGhz", "must be at least the base clock");
                }
            }
            case SpecsRequest.Cooler cooler -> checkCooler(cooler);
            default -> {
            }
        }
    }

    // Otherwise a CPU could be listed under Laptops, where customers would never look for it
    private static void checkCategory(ProductType type, String slug) {
        if (!type.categorySlug().equals(slug.trim())) {
            throw new InvalidInputException("categorySlug",
                    type + " products belong in '" + type.categorySlug() + "'");
        }
    }

    // Same rule as the CHECK constraint in V3: only liquid coolers have a radiator.
    // Air coolers need a height, because the cooler-height rule compares it with the case.
    private static void checkCooler(SpecsRequest.Cooler cooler) {
        if (cooler.coolerType() == CoolerType.AIR) {
            if (cooler.heightMm() == null) {
                throw new InvalidInputException("specs.heightMm", "air coolers need a height");
            }
            if (cooler.radiatorMm() != null) {
                throw new InvalidInputException("specs.radiatorMm", "air coolers have no radiator; leave this empty");
            }
        } else {
            if (cooler.radiatorMm() == null) {
                throw new InvalidInputException("specs.radiatorMm", "liquid coolers need a radiator size");
            }
            if (cooler.heightMm() != null) {
                throw new InvalidInputException("specs.heightMm", "only air coolers have a height; leave this empty");
            }
        }
    }

    private static void copyFields(ProductRequest request, Product product, String sku, Category category) {
        product.setSku(sku);
        product.setName(request.name().trim());
        product.setBrand(request.brand().trim());
        product.setDescription(blankToNull(request.description()));
        product.setImageUrl(blankToNull(request.imageUrl()));
        product.setPrice(request.price());
        product.setStockQuantity(request.stockQuantity());
        product.setCategory(category);
        product.setWarrantyMonths(request.warrantyMonths());
        product.setPerformanceScore(request.performanceScore());
    }

    private Product findProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product " + id + " not found"));
    }

    // 400 on the field (not 404): the product exists, only this value in the form is wrong
    private Category findCategory(String slug) {
        return categoryRepository.findBySlug(slug.trim())
                .orElseThrow(() -> new InvalidInputException("categorySlug", "No category called '" + slug + "'"));
    }

    private ProductDetailDto toDto(Product product) {
        return ProductDetailDto.from(product, productSpecsService.findSpecs(product));
    }

    private static ConflictException skuTaken(String sku) {
        return new ConflictException("Another product already uses SKU " + sku + ".");
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
