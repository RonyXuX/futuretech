package com.futuretech.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Services ask this Clock for "now" instead of calling LocalDateTime.now() directly,
 * so tests can use a fixed time (e.g. to check that a 15-minute lock really ends).
 */
@Configuration
public class TimeConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
