package com.finance.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple in-memory rate limiter for auth endpoints.
 * Limits each IP to a configurable number of requests per minute on /api/auth/** paths.
 * Prevents brute-force login attempts and token enumeration.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    @Value("${app.rate-limit.max-requests:10}")
    private int maxRequestsPerMinute;

    private static final long WINDOW_MS = 60_000;

    private final Map<String, RateBucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        if (!path.startsWith("/api/auth/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(request);
        RateBucket bucket = buckets.computeIfAbsent(clientIp, k -> new RateBucket());

        if (!bucket.tryConsume(maxRequestsPerMinute)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"status\":429,\"message\":\"Too many requests. Please try again later.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static class RateBucket {
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile long windowStart = System.currentTimeMillis();

        boolean tryConsume(int maxRequests) {
            long now = System.currentTimeMillis();
            if (now - windowStart > WINDOW_MS) {
                count.set(0);
                windowStart = now;
            }
            return count.incrementAndGet() <= maxRequests;
        }
    }
}
