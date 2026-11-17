package com.futuretech.service;

import static com.futuretech.forge.TestParts.build;
import static com.futuretech.forge.TestParts.cpu;
import static com.futuretech.forge.TestParts.gpu;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.futuretech.dto.forge.BuildCheckRequest.Item;
import com.futuretech.dto.forge.CompatibilityReport;
import com.futuretech.entity.Product;
import com.futuretech.entity.enums.ProductType;
import com.futuretech.exception.InvalidInputException;
import com.futuretech.forge.BuildParts;
import com.futuretech.forge.CompatibilityRule;
import com.futuretech.forge.Issue;
import com.futuretech.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class CompatibilityServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private BuildPartsLoader buildPartsLoader;

    private final List<CompatibilityRule> rules = new ArrayList<>();
    private CompatibilityService service;

    @BeforeEach
    void setUp() {
        service = new CompatibilityService(productRepository, buildPartsLoader, rules);
    }

    // ---------- report ----------

    @Test
    void emptyBuild_isCompatibleButNotComplete() {
        givenProducts();
        givenParts(build().parts());

        CompatibilityReport report = service.check(List.of());

        assertThat(report.compatible()).isTrue();
        assertThat(report.complete()).isFalse();
        assertThat(report.missingParts()).containsExactly(ProductType.CPU, ProductType.MOTHERBOARD,
                ProductType.RAM, ProductType.STORAGE, ProductType.PSU, ProductType.CASE, ProductType.COOLER);
        assertThat(report.estimatedWattage()).isNull();
        assertThat(report.totalPrice()).isEqualByComparingTo("0");
    }

    @Test
    void errorsAndWarnings_areSeparated_andOnlyErrorsBreakCompatibility() {
        rules.add(b -> List.of(Issue.error("A", "broken")));
        rules.add(b -> List.of(Issue.warning("B", "risky")));
        givenProducts();
        givenParts(build().parts());

        CompatibilityReport report = service.check(List.of());

        assertThat(report.compatible()).isFalse();
        assertThat(report.errors()).extracting(Issue::rule).containsExactly("A");
        assertThat(report.warnings()).extracting(Issue::rule).containsExactly("B");
    }

    @Test
    void onlyWarnings_isStillCompatible() {
        rules.add(b -> List.of(Issue.warning("B", "risky")));
        givenProducts();
        givenParts(build().parts());

        assertThat(service.check(List.of()).compatible()).isTrue();
    }

    @Test
    void everyRuleRuns() {
        rules.add(b -> List.of(Issue.error("A", "1")));
        rules.add(b -> List.of());
        rules.add(b -> List.of(Issue.error("C", "3"), Issue.error("C", "4")));
        givenProducts();
        givenParts(build().parts());

        assertThat(service.check(List.of()).errors()).hasSize(3);
    }

    @Test
    void gpuIsMissing_onlyWhenTheCpuHasNoIntegratedGraphics() {
        givenProducts(product(1L, ProductType.CPU, "300.00"));

        givenParts(build().cpu(cpu("AM5", 120, false)).parts());
        assertThat(service.check(items(1L)).missingParts()).contains(ProductType.GPU);

        givenParts(build().cpu(cpu("AM5", 120, true)).parts());
        assertThat(service.check(items(1L)).missingParts()).doesNotContain(ProductType.GPU);

        givenParts(build().cpu(cpu("AM5", 120, false)).gpu(gpu(300, 250)).parts());
        assertThat(service.check(items(1L)).missingParts()).doesNotContain(ProductType.GPU);
    }

    @Test
    void wattageEstimate_comesFromTheBuild() {
        givenProducts(product(1L, ProductType.CPU, "300.00"));
        givenParts(build().cpu(cpu("AM5", 120, false)).gpu(gpu(300, 250)).parts());

        assertThat(service.check(items(1L)).estimatedWattage()).isEqualTo(470);   // 120 + 250 + 100
    }

    @Test
    void totalPrice_usesQuantities() {
        givenProducts(product(1L, ProductType.CPU, "300.00"), product(2L, ProductType.RAM, "99.99"));
        givenParts(build().parts());

        CompatibilityReport report = service.check(List.of(new Item(1L, 1), new Item(2L, 2)));

        assertThat(report.totalPrice()).isEqualByComparingTo("499.98");
    }

    @Test
    void sameProductTwice_isMergedIntoOneLine() {
        givenProducts(product(2L, ProductType.RAM, "99.99"));
        givenParts(build().parts());

        service.check(List.of(new Item(2L, 1), new Item(2L, 1)));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<Long, Integer>> quantities = ArgumentCaptor.forClass(Map.class);
        verify(buildPartsLoader).load(any(), quantities.capture());
        assertThat(quantities.getValue()).containsExactly(Map.entry(2L, 2));
    }

    // ---------- bad input (400) ----------

    @Test
    void unknownProduct_isRejected() {
        givenProducts();
        assertRejected(items(999L), "999");
    }

    @Test
    void hiddenProduct_isRejected() {
        Product hidden = product(1L, ProductType.CPU, "300.00");
        hidden.setActive(false);
        givenProducts(hidden);
        assertRejected(items(1L), "no longer sold");
    }

    @Test
    void laptopInAPcBuild_isRejected() {
        givenProducts(product(1L, ProductType.LAPTOP, "999.00"));
        assertRejected(items(1L), "not a PC part");
    }

    @Test
    void twoDifferentCpus_areRejected() {
        givenProducts(product(1L, ProductType.CPU, "300.00"), product(2L, ProductType.CPU, "400.00"));
        assertRejected(items(1L, 2L), "only one CPU");
    }

    @Test
    void oneCpuWithQuantityTwo_isRejected() {
        givenProducts(product(1L, ProductType.CPU, "300.00"));
        assertRejected(List.of(new Item(1L, 2)), "only one CPU");
    }

    @Test
    void severalRamKitsAndDrives_areAllowed() {
        givenProducts(product(1L, ProductType.RAM, "99.99"), product(2L, ProductType.RAM, "89.99"),
                product(3L, ProductType.STORAGE, "79.99"), product(4L, ProductType.STORAGE, "59.99"));
        givenParts(build().parts());

        assertThat(service.check(List.of(new Item(1L, 2), new Item(2L, 1), new Item(3L, 1), new Item(4L, 3))))
                .isNotNull();
    }

    // ---------- helpers ----------

    private void assertRejected(List<Item> items, String messagePart) {
        assertThatThrownBy(() -> service.check(items))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining(messagePart)
                .extracting("field").isEqualTo("items");
    }

    private void givenProducts(Product... products) {
        when(productRepository.findAllById(any())).thenAnswer(call -> {
            Collection<Long> ids = call.getArgument(0);
            return List.of(products).stream().filter(p -> ids.contains(p.getId())).toList();
        });
    }

    private void givenParts(BuildParts parts) {
        when(buildPartsLoader.load(any(), anyMap())).thenReturn(parts);
    }

    private static List<Item> items(Long... ids) {
        return List.of(ids).stream().map(id -> new Item(id, 1)).toList();
    }

    private static Product product(Long id, ProductType type, String price) {
        Product p = new Product();
        p.setId(id);
        p.setName(type + " " + id);
        p.setProductType(type);
        p.setPrice(new BigDecimal(price));
        p.setActive(true);
        return p;
    }
}
