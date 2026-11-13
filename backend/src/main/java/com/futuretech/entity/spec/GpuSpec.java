package com.futuretech.entity.spec;

import com.futuretech.entity.Product;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "gpu_specs")
@Getter
@Setter
@NoArgsConstructor
public class GpuSpec {

    @Id
    private Long productId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false)
    private String chipset;

    @Column(nullable = false)
    private Integer vramGb;

    @Column(nullable = false)
    private Integer lengthMm;

    @Column(nullable = false)
    private Integer tdpWatts;
}
