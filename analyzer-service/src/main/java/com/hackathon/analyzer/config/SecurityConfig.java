package com.hackathon.analyzer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Security configuration for Analyzer Service
 *
 * For development: All endpoints are open
 * For production: API key or JWT authentication required
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Development profile - No authentication required
     */
    @Bean
    @Profile("dev")
    public SecurityFilterChain devSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            );

        return http.build();
    }

    /**
     * Production profile - API key authentication required
     *
     * Security features:
     * - CORS enabled for specific origins
     * - CSRF protection disabled (stateless REST API)
     * - Public endpoints: /api/health, /swagger-ui/**, /v3/api-docs/**
     * - Protected endpoints: All analysis and metrics endpoints
     */
    @Bean
    @Profile("prod")
    public SecurityFilterChain prodSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers(
                    "/api/health",
                    "/actuator/health",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/swagger-ui.html"
                ).permitAll()
                // Protected endpoints - require authentication
                .requestMatchers(
                    "/api/analyze/**",
                    "/api/metrics/**",
                    "/api/dashboard"
                ).authenticated()
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .httpBasic(httpBasic -> {});  // Simple HTTP Basic auth for demo

        return http.build();
    }

    /**
     * CORS configuration
     *
     * Allows requests from:
     * - localhost (development)
     * - GCP Cloud Run URLs (production)
     * - Firebase Hosting (dashboard)
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Allowed origins
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:8080",      // Dashboard local
            "http://localhost:3000",      // React dev server
            "https://*.run.app",          // GCP Cloud Run
            "https://*.web.app",          // Firebase Hosting
            "https://*.firebaseapp.com"   // Firebase Hosting
        ));

        // Allowed methods
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS"
        ));

        // Allowed headers
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-API-Key",
            "X-Requested-With"
        ));

        // Allow credentials
        configuration.setAllowCredentials(true);

        // Max age
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
