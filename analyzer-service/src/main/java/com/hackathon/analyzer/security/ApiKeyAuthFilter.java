package com.hackathon.analyzer.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * API Key authentication filter
 *
 * Validates X-API-Key header against configured API key
 * Only active in production profile
 */
@Slf4j
@Component
@Profile("prod")
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    @Value("${app.security.api-key:default-api-key-change-in-production}")
    private String apiKey;

    private static final String API_KEY_HEADER = "X-API-Key";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestApiKey = request.getHeader(API_KEY_HEADER);

        // Check if API key is present and valid
        if (requestApiKey != null && requestApiKey.equals(apiKey)) {
            // API key is valid - set authentication
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            "api-user",
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_API"))
                    );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("Valid API key provided for request: {}", request.getRequestURI());
        } else if (requestApiKey != null) {
            log.warn("Invalid API key provided for request: {}", request.getRequestURI());
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        // Don't filter public endpoints
        return path.startsWith("/api/health") ||
               path.startsWith("/actuator/health") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/v3/api-docs");
    }
}
