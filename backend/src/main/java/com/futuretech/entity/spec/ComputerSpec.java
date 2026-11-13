package com.futuretech.entity.spec;

import com.futuretech.entity.Product;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Summary specs for pre-built laptops and desktops (display and filtering only). */
@Entity
@Table(name = "computer_specs")
@Getter
@Setter
@NoArgsConstructor
public class ComputerSpec {

    @Id
    private Long productId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false)
    private String cpuModel;

    @Column(nullable = false)
    private String gpuModel;

    @Column(nullable = false)
    private Integer ramGb;

    @Column(nullable = false)
    private Integer storageGb;

    // Laptops only
    private Double screenSizeInches;

    private String operatingSystem;
}
