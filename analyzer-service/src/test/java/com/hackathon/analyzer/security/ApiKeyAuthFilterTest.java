package com.hackathon.analyzer.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiKeyAuthFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private ApiKeyAuthFilter filter;

    @BeforeEach
    void setUp() {
        filter = new ApiKeyAuthFilter();
        ReflectionTestUtils.setField(filter, "apiKey", "test-api-key");
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_withValidApiKey_shouldSetAuthentication() throws ServletException, IOException {
        when(request.getHeader("X-API-Key")).thenReturn("test-api-key");
        when(request.getRequestURI()).thenReturn("/api/analyze/test");

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .isEqualTo("api-user");

        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void doFilterInternal_withInvalidApiKey_shouldNotSetAuthentication() throws ServletException, IOException {
        when(request.getHeader("X-API-Key")).thenReturn("wrong-api-key");
        when(request.getRequestURI()).thenReturn("/api/analyze/test");

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void doFilterInternal_withoutApiKey_shouldNotSetAuthentication() throws ServletException, IOException {
        when(request.getHeader("X-API-Key")).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/api/analyze/test");

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void shouldNotFilter_healthEndpoint_shouldReturnTrue() {
        when(request.getRequestURI()).thenReturn("/api/health");

        boolean result = filter.shouldNotFilter(request);

        assertThat(result).isTrue();
    }

    @Test
    void shouldNotFilter_actuatorEndpoint_shouldReturnTrue() {
        when(request.getRequestURI()).thenReturn("/actuator/health");

        boolean result = filter.shouldNotFilter(request);

        assertThat(result).isTrue();
    }

    @Test
    void shouldNotFilter_protectedEndpoint_shouldReturnFalse() {
        when(request.getRequestURI()).thenReturn("/api/analyze/test");

        boolean result = filter.shouldNotFilter(request);

        assertThat(result).isFalse();
    }
}
