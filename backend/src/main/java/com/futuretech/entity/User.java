package com.futuretech.entity;

import java.time.Duration;
import java.time.LocalDateTime;

import org.hibernate.annotations.DynamicUpdate;

import com.futuretech.entity.enums.Role;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// "user" is a reserved word in PostgreSQL
@Entity
@Table(name = "users")
// Only write the columns that actually changed. Otherwise saving e.g. a failed-login counter
// would also write back an old points balance and undo points added at the same moment.
@DynamicUpdate
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.CUSTOMER;

    @Column(nullable = false)
    private Integer pointsBalance = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private Integer failedLoginAttempts = 0;

    private LocalDateTime lockedUntil;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public boolean isLockedAt(LocalDateTime now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    public void recordFailedLogin(LocalDateTime now, int maxAttempts, Duration lockDuration) {
        failedLoginAttempts++;
        if (failedLoginAttempts >= maxAttempts) {
            lockedUntil = now.plus(lockDuration);
            failedLoginAttempts = 0;  // start counting again after the lock ends
        }
    }

    public void recordSuccessfulLogin() {
        failedLoginAttempts = 0;
        lockedUntil = null;
    }
}
