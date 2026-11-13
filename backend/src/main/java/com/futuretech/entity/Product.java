package com.futuretech.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.futuretech.entity.enums.ProductType;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Anything we sell. Type-specific details (socket, wattage, ...) live in a separate
 * spec entity (CpuSpec, GpuSpec, ...) that points back to this product.
 */
@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String sku;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String brand;

    private String description;

    private String imageUrl;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer stockQuantity = 0;

    // LAZY: the category is only loaded from the DB when getCategory() is actually used
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id")
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductType productType;

    @Column(nullable = false)
    private Integer warrantyMonths = 12;

    // 0-100, compared only between products of the same type (used by the budget optimizer)
    private Integer performanceScore;

    // Products are never deleted (old orders point to them); inactive ones are hidden
    @Column(nullable = false)
    private Boolean active = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
