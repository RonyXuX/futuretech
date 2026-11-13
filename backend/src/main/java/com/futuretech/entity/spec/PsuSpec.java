package com.futuretech.entity.spec;

import com.futuretech.entity.Product;
import com.futuretech.entity.enums.PsuFormFactor;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "psu_specs")
@Getter
@Setter
@NoArgsConstructor
public class PsuSpec {

    @Id
    private Long productId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false)
    private Integer wattage;

    @Column(nullable = false)
    private String efficiencyRating;

    @Column(nullable = false)
    private Boolean modular;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PsuFormFactor formFactor = PsuFormFactor.ATX;
}
