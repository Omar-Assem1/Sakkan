package com.sakan.support;

import org.springframework.jdbc.core.JdbcTemplate;

/** Helpers for tests that persist entities referencing fixed user IDs. */
public final class TestUserFixtures {

    private TestUserFixtures() {}

    public static void ensureUsers(JdbcTemplate jdbc, long... userIds) {
        for (long userId : userIds) {
            jdbc.update("""
                    INSERT INTO users (id, username, email, auth_provider, is_seeded_admin, enabled)
                    VALUES (?, ?, ?, 'LOCAL', FALSE, TRUE)
                    ON CONFLICT (id) DO NOTHING
                    """, userId, username(userId), email(userId));
        }

        jdbc.queryForObject("""
                SELECT setval(
                    pg_get_serial_sequence('users', 'id'),
                    GREATEST(COALESCE((SELECT MAX(id) FROM users), 1), 1),
                    TRUE
                )
                """, Long.class);
    }

    public static void deleteUsers(JdbcTemplate jdbc, long... userIds) {
        for (long userId : userIds) {
            jdbc.update("DELETE FROM users WHERE id = ? AND username = ?", userId, username(userId));
        }
    }

    private static String username(long userId) {
        return "test_fixture_user_" + userId;
    }

    private static String email(long userId) {
        return username(userId) + "@sakan.test";
    }
}
