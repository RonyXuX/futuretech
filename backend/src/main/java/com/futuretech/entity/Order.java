package com.futuretech.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.futuretech.entity.enums.OrderStatus;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// "order" is a reserved word in SQL
@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PLACED;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    // Shipping details are copied at checkout so old orders never change
    @Column(nullable = false)
    private String shippingFullName;

    @Column(nullable = false)
    private String shippingAddress;

    @Column(nullable = false)
    private String shippingCity;

    @Column(nullable = false)
    private String shippingPhone;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    /** Adds an item and sets both sides of the relationship. */
    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
