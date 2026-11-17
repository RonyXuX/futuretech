package com.futuretech.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.futuretech.dto.forge.BuildCheckRequest;
import com.futuretech.dto.forge.CompatibilityReport;
import com.futuretech.entity.Product;
import com.futuretech.entity.enums.ProductType;
import com.futuretech.exception.InvalidInputException;
import com.futuretech.forge.BuildParts;
import com.futuretech.forge.CompatibilityRule;
import com.futuretech.forge.Issue;
import com.futuretech.forge.Severity;
import com.futuretech.forge.rules.PsuWattageRule;
import com.futuretech.repository.ProductRepository;

/**
 * ForgeAI's rule engine: checks a list of parts with every CompatibilityRule.
 * Used by the manual builder now, and later by saved builds and to validate AI suggestions.
 */
@Service
public class CompatibilityService {

    /** A PC has exactly one of these; RAM and storage can be several. */
    private static final Set<ProductType> ONE_PER_BUILD = EnumSet.of(
            ProductType.CPU, ProductType.MOTHERBOARD, ProductType.GPU,
            ProductType.PSU, ProductType.CASE, ProductType.COOLER);
    private static final Set<ProductType> PC_PARTS = EnumSet.of(
            ProductType.CPU, ProductType.MOTHERBOARD, ProductType.GPU, ProductType.PSU, ProductType.CASE,
            ProductType.COOLER, ProductType.RAM, ProductType.STORAGE);
    // GPU is not here: it's only needed when the CPU has no integrated graphics (see missingParts)
    private static final List<ProductType> ALWAYS_REQUIRED = List.of(
            ProductType.CPU, ProductType.MOTHERBOARD, ProductType.RAM, ProductType.STORAGE,
            ProductType.PSU, ProductType.CASE, ProductType.COOLER);

    private final ProductRepository productRepository;
    private final BuildPartsLoader buildPartsLoader;
    private final List<CompatibilityRule> rules;

    /** Spring passes every CompatibilityRule bean, so a new rule class is picked up automatically. */
    public CompatibilityService(ProductRepository productRepository, BuildPartsLoader buildPartsLoader,
                                List<CompatibilityRule> rules) {
        this.productRepository = productRepository;
        this.buildPartsLoader = buildPartsLoader;
        this.rules = rules;
    }

    @Transactional(readOnly = true)
    public CompatibilityReport check(List<BuildCheckRequest.Item> items) {
        Map<Long, Integer> quantities = mergeQuantities(items);
        List<Product> products = loadProducts(quantities);
        checkStructure(products, quantities);
        BuildParts build = buildPartsLoader.load(products, quantities);

        List<Issue> issues = rules.stream().flatMap(rule -> rule.check(build).stream()).toList();
        List<Issue> errors = issues.stream().filter(i -> i.severity() == Severity.ERROR).toList();
        List<Issue> warnings = issues.stream().filter(i -> i.severity() == Severity.WARNING).toList();
        List<ProductType> missing = missingParts(build);

        return new CompatibilityReport(errors.isEmpty(), missing.isEmpty(), errors, warnings, missing,
                PsuWattageRule.estimatedDraw(build), totalPrice(products, quantities));
    }

    /** The same product twice in the list counts as one line with the quantities added up. */
    private static Map<Long, Integer> mergeQuantities(List<BuildCheckRequest.Item> items) {
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (BuildCheckRequest.Item item : items) {
            quantities.merge(item.productId(), item.quantity(), Integer::sum);
        }
        return quantities;
    }

    /** Sorted like the request, so messages and the report keep the customer's order. */
    private List<Product> loadProducts(Map<Long, Integer> quantities) {
        Map<Long, Product> byId = productRepository.findAllById(quantities.keySet()).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        List<Product> products = new ArrayList<>();
        for (Long id : quantities.keySet()) {
            Product product = byId.get(id);
            // hidden products count as missing: they can't be bought, so they can't be in a build
            if (product == null || !product.getActive()) {
                throw new InvalidInputException("items", "Product " + id + " doesn't exist or is no longer sold.");
            }
            products.add(product);
        }
        return products;
    }

    /** A wrong build shape (two CPUs, a mouse in a PC) is bad input, not an incompatibility. */
    private static void checkStructure(List<Product> products, Map<Long, Integer> quantities) {
        Map<ProductType, Integer> perType = new LinkedHashMap<>();
        for (Product p : products) {
            ProductType type = p.getProductType();
            if (!PC_PARTS.contains(type)) {
                throw new InvalidInputException("items",
                        p.getName() + " is a " + type + ", not a PC part, so it can't be in a build.");
            }
            perType.merge(type, quantities.get(p.getId()), Integer::sum);
        }
        perType.forEach((type, count) -> {
            if (ONE_PER_BUILD.contains(type) && count > 1) {
                throw new InvalidInputException("items", "A build can have only one " + type + ".");
            }
        });
    }

    private static List<ProductType> missingParts(BuildParts build) {
        List<ProductType> missing = new ArrayList<>();
        for (ProductType type : ALWAYS_REQUIRED) {
            boolean present = switch (type) {
                case CPU -> build.cpu() != null;
                case MOTHERBOARD -> build.motherboard() != null;
                case RAM -> !build.ram().isEmpty();
                case STORAGE -> !build.storage().isEmpty();
                case PSU -> build.psu() != null;
                case CASE -> build.pcCase() != null;
                case COOLER -> build.cooler() != null;
                default -> true;
            };
            if (!present) {
                missing.add(type);
            }
        }
        // only decidable once the CPU is known: with integrated graphics the PC runs without a card
        if (build.cpu() != null && build.gpu() == null && !build.cpu().spec().getIntegratedGraphics()) {
            missing.add(ProductType.GPU);
        }
        return missing;
    }

    private static BigDecimal totalPrice(List<Product> products, Map<Long, Integer> quantities) {
        return products.stream()
                .map(p -> p.getPrice().multiply(BigDecimal.valueOf(quantities.get(p.getId()))))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
