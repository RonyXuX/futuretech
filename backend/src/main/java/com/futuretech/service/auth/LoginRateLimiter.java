package com.futuretech.service.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

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
    private final AtomicReference<Instant> lastCleanup;
    private final int maxAttempts;
    private final Clock clock;

    public LoginRateLimiter(SecurityProperties props, Clock clock) {
        this.maxAttempts = props.loginAttemptsPerMinute();
        this.clock = clock;
        this.lastCleanup = new AtomicReference<>(clock.instant());
    }

    private record Window(Instant start, int count) {
        boolean isOver(Instant now) {
            return !now.isBefore(start.plus(WINDOW));
        }
    }

    public void checkAllowed(String key) {
        Instant now = clock.instant();
        removeOldWindows(now);
        // compute() runs atomically per key, so two parallel requests can't both slip through
        Window window = windows.compute(key, (k, w) ->
                (w == null || w.isOver(now))
                        ? new Window(now, 1)               // first attempt, or the minute is over
                        : new Window(w.start(), w.count() + 1));
        if (window.count() > maxAttempts) {
            throw AuthException.tooManyAttempts(Duration.between(now, window.start().plus(WINDOW)));
        }
    }

    /**
     * Without this, every address that ever tried to log in would stay in memory until a restart.
     * A finished window means nothing any more, so it can go. Runs at most once per minute, so a
     * busy server doesn't scan the whole map on every login.
     */
    private void removeOldWindows(Instant now) {
        Instant last = lastCleanup.get();
        // compareAndSet: if several logins arrive at once, only one of them does the cleanup
        if (now.isBefore(last.plus(WINDOW)) || !lastCleanup.compareAndSet(last, now)) {
            return;
        }
        // ConcurrentHashMap removes an entry only if it is still the one we checked, so a window
        // that another request just started is never lost
        windows.values().removeIf(w -> w.isOver(now));
    }

    /** For tests: how many addresses are currently remembered. */
    int trackedKeys() {
        return windows.size();
    }
}
