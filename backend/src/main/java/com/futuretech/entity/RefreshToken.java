package com.futuretech.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Only the SHA-256 hash is stored; the raw token exists only in the user's cookie. */
@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, unique = true)
    private String tokenHash;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Set when the token is used (rotated) or the user logs out
    private LocalDateTime revokedAt;

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpiredAt(LocalDateTime now) {
        return !expiresAt.isAfter(now);
    }
}
