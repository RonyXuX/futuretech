package com.futuretech.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class UserLockoutTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 13, 12, 0);
    private static final Duration LOCK = Duration.ofMinutes(15);

    @Test
    void fourWrongPasswords_doNotLock() {
        User user = new User();
        for (int i = 0; i < 4; i++) {
            user.recordFailedLogin(NOW, 5, LOCK);
        }
        assertThat(user.isLockedAt(NOW)).isFalse();
        assertThat(user.getFailedLoginAttempts()).isEqualTo(4);
    }

    @Test
    void fifthWrongPassword_locksFor15Minutes() {
        User user = new User();
        for (int i = 0; i < 5; i++) {
            user.recordFailedLogin(NOW, 5, LOCK);
        }
        assertThat(user.isLockedAt(NOW)).isTrue();
        assertThat(user.isLockedAt(NOW.plusMinutes(14))).isTrue();
        assertThat(user.isLockedAt(NOW.plusMinutes(15))).isFalse();   // lock has ended
        assertThat(user.getFailedLoginAttempts()).isZero();           // counting starts again
    }

    @Test
    void successfulLogin_resetsCounterAndLock() {
        User user = new User();
        user.recordFailedLogin(NOW, 5, LOCK);
        user.recordFailedLogin(NOW, 5, LOCK);

        user.recordSuccessfulLogin();

        assertThat(user.getFailedLoginAttempts()).isZero();
        assertThat(user.getLockedUntil()).isNull();
    }
}
