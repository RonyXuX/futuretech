package com.futuretech.entity.spec;

import com.futuretech.entity.Product;
import com.futuretech.entity.enums.FormFactor;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** PC case clearances used by the Case-size, GPU-length and Cooler-height rules. */
@Entity
@Table(name = "case_specs")
@Getter
@Setter
@NoArgsConstructor
public class CaseSpec {

    @Id
    private Long productId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "product_id")
    private Product product;

    // Largest motherboard that fits; smaller form factors fit too
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FormFactor maxMotherboardFormFactor;

    @Column(nullable = false)
    private Integer maxGpuLengthMm;

    @Column(nullable = false)
    private Integer maxCoolerHeightMm;
}
