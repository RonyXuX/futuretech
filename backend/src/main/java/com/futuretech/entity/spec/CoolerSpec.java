package com.futuretech.entity.spec;

import java.util.HashSet;
import java.util.Set;

import com.futuretech.entity.Product;
import com.futuretech.entity.enums.CoolerType;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** CPU cooler details used by the Cooler-height and Socket rules. */
@Entity
@Table(name = "cooler_specs")
@Getter
@Setter
@NoArgsConstructor
public class CoolerSpec {

    @Id
    private Long productId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "product_id")
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CoolerType coolerType;

    // Only set for AIR coolers (liquid coolers mount a radiator instead)
    private Integer heightMm;

    // A simple list of strings stored in its own table (cooler_supported_sockets)
    @ElementCollection
    @CollectionTable(name = "cooler_supported_sockets", joinColumns = @JoinColumn(name = "cooler_id"))
    @Column(name = "socket", nullable = false)
    private Set<String> supportedSockets = new HashSet<>();
}
