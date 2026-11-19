package com.sakan.auth.service;

import com.sakan.common.config.AdminSeedProperties;
import com.sakan.user.service.UserService;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the single hardcoded admin at startup. Idempotent: does nothing when a seeded admin exists.
 * Never logs the username, password or hash.
 */
@Slf4j
@Component
public class SeededAdminRunner implements ApplicationRunner {

    static final String DEFAULT_EMAIL = "admin@sakan.local";

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final AdminSeedProperties properties;

    public SeededAdminRunner(UserService userService, PasswordEncoder passwordEncoder,
                             AdminSeedProperties properties) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        String username = trimToNull(properties.username());
        if (username == null) {
            throw new IllegalStateException("ADMIN_USERNAME is not set. The seeded administrator cannot be created.");
        }
        String password = properties.password();
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("ADMIN_PASSWORD is not set. The seeded administrator cannot be created.");
        }
        String configuredEmail = trimToNull(properties.email());
        String email = configuredEmail == null ? DEFAULT_EMAIL : configuredEmail.toLowerCase(Locale.ROOT);

        if (userService.existsSeededAdmin()) {
            log.info("Seeded administrator already exists; nothing to do.");
            return;
        }
        // Never turn an existing normal account into the admin.
        if (userService.existsByUsername(username)) {
            throw new IllegalStateException(
                    "A non-admin account already uses the configured ADMIN_USERNAME. Change ADMIN_USERNAME or rename that account.");
        }
        if (userService.existsByEmail(email)) {
            throw new IllegalStateException(
                    "A non-admin account already uses the configured ADMIN_EMAIL. Change ADMIN_EMAIL or that account's email.");
        }

        try {
            userService.createSeededAdmin(username, email, passwordEncoder.encode(password));
            log.info("Seeded administrator account created.");
        } catch (DataIntegrityViolationException e) {
            // Another instance won the race: the V11 unique index allows only one seeded admin.
            if (userService.existsSeededAdmin()) {
                log.info("Seeded administrator already exists; nothing to do.");
                return;
            }
            throw e;
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}