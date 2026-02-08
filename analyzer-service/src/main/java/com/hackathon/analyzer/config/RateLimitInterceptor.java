package com.hackathon.analyzer.config;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * HTTP interceptor that applies tiered rate limiting to incoming requests.
 * Analysis and scaling endpoints get a stricter budget; auth gets brute-force
 * protection.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiter standardRateLimiter;
    private final RateLimiter analysisRateLimiter;
    private final RateLimiter authRateLimiter;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String path = request.getRequestURI();

        if (path.startsWith("/api/health") || path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs")) {
            return true;
        }

        RateLimiter limiter = resolveRateLimiter(path);

        try {
            RateLimiter.waitForPermission(limiter);
            return true;
        } catch (RequestNotPermitted e) {
            log.warn("Rate limit exceeded for path: {} from IP: {}", path, request.getRemoteAddr());
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Please retry after a short delay.\",\"retryAfterMs\":1000}");
            return false;
        }
    }

    private RateLimiter resolveRateLimiter(String path) {
        if (path.startsWith("/api/auth")) {
            return authRateLimiter;
        }
        if (path.contains("/analyze") || path.contains("/scaling") || path.contains("/predict")
                || path.contains("/classify")) {
            return analysisRateLimiter;
        }
        return standardRateLimiter;
    }
}
