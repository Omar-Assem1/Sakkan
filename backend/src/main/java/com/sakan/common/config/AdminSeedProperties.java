package com.sakan.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Seeded admin settings from ADMIN_USERNAME, ADMIN_PASSWORD and ADMIN_EMAIL. No toString on purpose: a record would print the password. */
@ConfigurationProperties(prefix = "app.admin")
public record AdminSeedProperties(String username, String password, String email) {

    @Override
    public String toString() {
        return "AdminSeedProperties[redacted]";
    }
}