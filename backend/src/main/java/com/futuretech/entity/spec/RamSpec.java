package com.futuretech.entity.spec;

import com.futuretech.entity.Product;
import com.futuretech.entity.enums.RamType;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ram_specs")
@Getter
@Setter
@NoArgsConstructor
public class RamSpec {

    @Id
    private Long productId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "product_id")
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RamType ramType;

    // whole kit, e.g. 32 for 2x16GB
    @Column(nullable = false)
    private Integer capacityGb;

    // number of sticks
    @Column(nullable = false)
    private Integer modules;

    @Column(nullable = false)
    private Integer speedMhz;
}
