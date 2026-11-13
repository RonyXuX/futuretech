package com.futuretech.entity.spec;

import com.futuretech.entity.Product;
import com.futuretech.entity.enums.FormFactor;
import com.futuretech.entity.enums.RamType;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Motherboard details used by the Socket, RAM-type and Case-size rules. */
@Entity
@Table(name = "motherboard_specs")
@Getter
@Setter
@NoArgsConstructor
public class MotherboardSpec {

    @Id
    private Long productId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false)
    private String socket;

    @Column(nullable = false)
    private String chipset;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FormFactor formFactor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RamType ramType;

    @Column(nullable = false)
    private Integer ramSlots;

    @Column(nullable = false)
    private Integer maxRamGb;

    // Explicit name: the default naming would produce "m2slots" (no underscore after a digit)
    @Column(name = "m2_slots", nullable = false)
    private Integer m2Slots;
}
