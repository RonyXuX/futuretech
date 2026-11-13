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

/** A PC build a customer saved in ForgeAI (manual builder or AI suggestion). */
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

    // e.g. "gaming", "editing", "office"
    private String useCase;

    @Column(precision = 10, scale = 2)
    private BigDecimal budget;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Saving a build also saves its parts; removing a part from the list deletes it
    @OneToMany(mappedBy = "build", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BuildItem> items = new ArrayList<>();

    /** Adds a part and sets both sides of the relationship. */
    public void addItem(BuildItem item) {
        items.add(item);
        item.setBuild(this);
    }

    /** Total at TODAY's prices (not stored, so it never goes stale). */
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
