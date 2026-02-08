package com.hackathon.analyzer.config;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configures per-endpoint rate limiters using Resilience4j.
 * Three tiers: standard reads, expensive analysis operations, and auth
 * endpoints.
 */
@Configuration
public class RateLimiterConfiguration {

    @Bean
    public RateLimiterRegistry rateLimiterRegistry() {
        return RateLimiterRegistry.of(defaultConfig());
    }

    @Bean
    public RateLimiter standardRateLimiter(RateLimiterRegistry registry) {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .limitForPeriod(20)
                .timeoutDuration(Duration.ofMillis(500))
                .build();
        return registry.rateLimiter("standard", config);
    }

    @Bean
    public RateLimiter analysisRateLimiter(RateLimiterRegistry registry) {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofSeconds(10))
                .limitForPeriod(5)
                .timeoutDuration(Duration.ofMillis(1000))
                .build();
        return registry.rateLimiter("analysis", config);
    }

    @Bean
    public RateLimiter authRateLimiter(RateLimiterRegistry registry) {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .limitForPeriod(10)
                .timeoutDuration(Duration.ofMillis(200))
                .build();
        return registry.rateLimiter("auth", config);
    }

    private RateLimiterConfig defaultConfig() {
        return RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .limitForPeriod(50)
                .timeoutDuration(Duration.ofMillis(500))
                .build();
    }
}
