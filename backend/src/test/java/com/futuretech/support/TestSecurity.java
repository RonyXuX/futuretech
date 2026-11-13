package com.futuretech.support;

import java.time.Duration;
import java.util.List;

import com.futuretech.config.SecurityProperties;

/** Shared security settings for unit tests (same values as application.properties). */
public final class TestSecurity {

    public static final String JWT_SECRET = "test-only-secret-that-is-at-least-32-bytes-long";

    /** Use on web tests: @TestPropertySource(properties = TestSecurity.JWT_SECRET_PROPERTY) */
    public static final String JWT_SECRET_PROPERTY = "futuretech.security.jwt-secret=" + JWT_SECRET;

    private TestSecurity() {
    }

    public static SecurityProperties properties() {
        return new SecurityProperties(JWT_SECRET, Duration.ofMinutes(15), Duration.ofDays(7), false,
                5, Duration.ofMinutes(15), 10, List.of("http://localhost:4200"));
    }
}
