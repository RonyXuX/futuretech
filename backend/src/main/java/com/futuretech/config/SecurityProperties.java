package com.futuretech.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("futuretech.security")
public record SecurityProperties(
        String jwtSecret,                 // at least 32 characters
        Duration accessTokenLifetime,
        Duration refreshTokenLifetime,
        boolean cookieSecure,             // true in production: cookie only sent over HTTPS
        int maxFailedLogins,
        Duration lockDuration,
        int loginAttemptsPerMinute,       // per IP address
        List<String> corsAllowedOrigins) {
}
