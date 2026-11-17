package com.futuretech.service.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.futuretech.exception.AuthException;
import com.futuretech.support.MutableClock;
import com.futuretech.support.TestSecurity;

class LoginRateLimiterTest {

    private final MutableClock clock = new MutableClock(Instant.parse("2026-11-13T12:00:00Z"));
    private final LoginRateLimiter limiter = new LoginRateLimiter(TestSecurity.properties(), clock); // 10 per minute

    @Test
    void tenAttemptsPerMinute_areAllowed() {
        for (int i = 0; i < 10; i++) {
            limiter.checkAllowed("login:1.2.3.4");
        }
    }

    @Test
    void eleventhAttempt_isRejectedWith429AndRetryAfter() {
        for (int i = 0; i < 10; i++) {
            limiter.checkAllowed("login:1.2.3.4");
        }
        clock.advance(Duration.ofSeconds(20));

        assertThatThrownBy(() -> limiter.checkAllowed("login:1.2.3.4"))
                .isInstanceOfSatisfying(AuthException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                    assertThat(ex.getRetryAfter()).isEqualTo(Duration.ofSeconds(40));  // rest of the minute
                });
    }

    @Test
    void afterTheMinute_attemptsAreAllowedAgain() {
        for (int i = 0; i < 11; i++) {
            try {
                limiter.checkAllowed("login:1.2.3.4");
            } catch (AuthException ignored) {
                // the 11th is blocked
            }
        }
        clock.advance(Duration.ofMinutes(1));

        assertThatCode(() -> limiter.checkAllowed("login:1.2.3.4")).doesNotThrowAnyException();
    }

    @Test
    void differentAddresses_haveSeparateLimits() {
        for (int i = 0; i < 10; i++) {
            limiter.checkAllowed("login:1.2.3.4");
        }
        assertThatCode(() -> limiter.checkAllowed("login:5.6.7.8")).doesNotThrowAnyException();
    }

    // ---------- memory cleanup ----------

    @Test
    void finishedWindows_areForgotten() {
        for (int i = 0; i < 1000; i++) {
            limiter.checkAllowed("login:10.0." + (i / 256) + "." + (i % 256));
        }
        assertThat(limiter.trackedKeys()).isEqualTo(1000);

        clock.advance(Duration.ofMinutes(1));
        limiter.checkAllowed("login:1.2.3.4");

        assertThat(limiter.trackedKeys()).isEqualTo(1);  // only the new one is left
    }

    @Test
    void cleanup_keepsWindowsThatAreStillRunning() {
        for (int i = 0; i < 10; i++) {
            limiter.checkAllowed("login:1.2.3.4");  // uses up the whole minute
        }
        clock.advance(Duration.ofSeconds(59));
        limiter.checkAllowed("login:5.6.7.8");
        clock.advance(Duration.ofSeconds(2));       // 1.2.3.4's window is over, 5.6.7.8's isn't

        limiter.checkAllowed("login:9.9.9.9");      // triggers the cleanup

        assertThat(limiter.trackedKeys()).isEqualTo(2);  // 5.6.7.8 and 9.9.9.9
    }

    @Test
    void cleanup_neverResetsABlockedAddressEarly() {
        for (int i = 0; i < 10; i++) {
            limiter.checkAllowed("login:1.2.3.4");
        }
        clock.advance(Duration.ofSeconds(30));
        limiter.checkAllowed("login:5.6.7.8");      // other traffic in the same minute

        assertThatThrownBy(() -> limiter.checkAllowed("login:1.2.3.4")).isInstanceOf(AuthException.class);
    }
}
