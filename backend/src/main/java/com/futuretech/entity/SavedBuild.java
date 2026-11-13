package com.futuretech.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.futuretech.entity.enums.BuildSource;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "saved_builds")
@Getter
@Setter
@NoArgsConstructor
public class SavedBuild {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BuildSource source;

    private String useCase;

    @Column(precision = 10, scale = 2)
    private BigDecimal budget;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "build", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BuildItem> items = new ArrayList<>();

    /** Adds a part and sets both sides of the relationship. */
    public void addItem(BuildItem item) {
        items.add(item);
        item.setBuild(this);
    }

    /** At today's prices; not stored, so it never goes stale. */
    public BigDecimal getCurrentTotal() {
        return items.stream()
                .map(i -> i.getProduct().getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
