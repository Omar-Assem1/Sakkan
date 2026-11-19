package com.sakan.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.util.Set;
/** Blocks repeated failed logins from one client address with 429 (problem+json). */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final String BODY = "{\"type\":\"about:blank\",\"title\":\"Too Many Requests\","
            + "\"status\":429,\"detail\":\"Too many failed login attempts. Try again later.\"}";

    private final LoginAttemptLimiter limiter;

    public LoginRateLimitFilter(LoginAttemptLimiter limiter) {
        this.limiter = limiter;
    }

    private static final Set<String> LOGIN_PATHS = Set.of("/api/auth/login", "/api/auth/admin/login");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("POST".equals(request.getMethod()) && LOGIN_PATHS.contains(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        // Socket address on purpose: X-Forwarded-For is client-controlled and spoofable.
        String key = request.getRemoteAddr();

        if (limiter.isBlocked(key)) {
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(limiter.retryAfterSeconds(key)));
            response.setContentType("application/problem+json");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write(BODY);
            return;
        }

        chain.doFilter(request, response);

        int status = response.getStatus();
        if (status == HttpServletResponse.SC_UNAUTHORIZED) {
            limiter.recordFailure(key);
        } else if (status == HttpServletResponse.SC_OK) {
            limiter.recordSuccess(key);
        }
    }
}