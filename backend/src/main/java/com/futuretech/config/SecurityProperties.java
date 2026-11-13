package com.futuretech.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * All security settings in one typed object, read from the "futuretech.security.*" lines
 * in application.properties (durations are written like 15m, 7d).
 */
@ConfigurationProperties("futuretech.security")
public record SecurityProperties(
        String jwtSecret,                 // from the JWT_SECRET env var, at least 32 characters
        Duration accessTokenLifetime,     // JWT, e.g. 15m
        Duration refreshTokenLifetime,    // refresh cookie, e.g. 7d
        boolean cookieSecure,             // true = cookie only sent over HTTPS (set true in production)
        int maxFailedLogins,              // wrong passwords in a row before the account locks
        Duration lockDuration,            // how long the account stays locked
        int loginAttemptsPerMinute,       // per IP address
        List<String> corsAllowedOrigins) {
}
