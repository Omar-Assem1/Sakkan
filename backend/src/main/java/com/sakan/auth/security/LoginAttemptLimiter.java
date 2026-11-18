package com.sakan.auth.security;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** In-memory failed-login counter per client key. Single instance only (fine for M1). */
@Component
public class LoginAttemptLimiter {

    private record State(int failures, Instant windowStart) {
    }

    private final ConcurrentHashMap<String, State> attempts = new ConcurrentHashMap<>();
    private final int maxAttempts;
    private final Duration window;

    public LoginAttemptLimiter(
            @Value("${app.security.login-rate-limit.max-attempts:5}") int maxAttempts,
            @Value("${app.security.login-rate-limit.window-minutes:15}") long windowMinutes) {
        this.maxAttempts = maxAttempts;
        this.window = Duration.ofMinutes(windowMinutes);
    }

    public boolean isBlocked(String key) {
        State s = attempts.get(key);
        if (s == null) {
            return false;
        }
        if (Instant.now().isAfter(s.windowStart().plus(window))) {
            attempts.remove(key);
            return false;
        }
        return s.failures() >= maxAttempts;
    }

    public void recordFailure(String key) {
        Instant now = Instant.now();
        attempts.merge(key, new State(1, now), (old, ignored) ->
                now.isAfter(old.windowStart().plus(window))
                        ? new State(1, now)
                        : new State(old.failures() + 1, old.windowStart()));
    }

    public void recordSuccess(String key) {
        attempts.remove(key);
    }

    public long retryAfterSeconds(String key) {
        State s = attempts.get(key);
        if (s == null) {
            return 0;
        }
        return Math.max(1, Duration.between(Instant.now(), s.windowStart().plus(window)).getSeconds());
    }

    public void reset() {
        attempts.clear();
    }
}