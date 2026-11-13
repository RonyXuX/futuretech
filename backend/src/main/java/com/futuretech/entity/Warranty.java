package com.futuretech.entity;

import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Warranty for one purchased order line. Created at checkout from product.warrantyMonths. */
@Entity
@Table(name = "warranties")
@Getter
@Setter
@NoArgsConstructor
public class Warranty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_item_id", unique = true)
    private OrderItem orderItem;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate expiryDate;

    /** Status is computed, not stored, so it can never be out of date. */
    public boolean isActiveOn(LocalDate date) {
        return !date.isAfter(expiryDate);
    }
}
