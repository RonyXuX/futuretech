package com.futuretech.entity.spec;

import com.futuretech.entity.Product;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * CPU details used by the Socket and PSU-wattage rules.
 *
 * All spec entities follow the same pattern: @MapsId makes the product's id
 * also the primary key of this table, so each product has at most one spec row.
 */
@Entity
@Table(name = "cpu_specs")
@Getter
@Setter
@NoArgsConstructor
public class CpuSpec {

    @Id
    private Long productId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false)
    private String socket;

    @Column(nullable = false)
    private Integer cores;

    @Column(nullable = false)
    private Integer threads;

    @Column(nullable = false)
    private Double baseClockGhz;

    private Double boostClockGhz;

    @Column(nullable = false)
    private Integer tdpWatts;

    @Column(nullable = false)
    private Boolean integratedGraphics;
}
