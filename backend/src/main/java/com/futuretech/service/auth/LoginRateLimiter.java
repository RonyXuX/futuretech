package com.futuretech.service.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.futuretech.config.SecurityProperties;
import com.futuretech.exception.AuthException;

/**
 * Per-IP fixed window: catches one machine trying many accounts, which the per-account lock misses.
 * In memory, so it only works for a single server (several would need e.g. Redis).
 */
@Component
public class LoginRateLimiter {

    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final int maxAttempts;
    private final Clock clock;

    public LoginRateLimiter(SecurityProperties props, Clock clock) {
        this.maxAttempts = props.loginAttemptsPerMinute();
        this.clock = clock;
    }

    private record Window(Instant start, int count) {
    }

    public void checkAllowed(String key) {
        Instant now = clock.instant();
        // compute() runs atomically per key, so two parallel requests can't both slip through
        Window window = windows.compute(key, (k, w) ->
                (w == null || !now.isBefore(w.start().plus(WINDOW)))
                        ? new Window(now, 1)               // first attempt, or the minute is over
                        : new Window(w.start(), w.count() + 1));
        if (window.count() > maxAttempts) {
            throw AuthException.tooManyAttempts(Duration.between(now, window.start().plus(WINDOW)));
        }
    }
}
