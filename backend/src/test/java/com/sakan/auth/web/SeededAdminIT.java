package com.sakan.auth.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sakan.auth.service.SeededAdminRunner;
import com.sakan.support.AbstractIntegrationTest;
import com.sakan.support.tags.RegressionTest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@RegressionTest
@Transactional
class SeededAdminIT extends AbstractIntegrationTest {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private SeededAdminRunner runner;

    @Value("${app.admin.username}") private String adminUsername;
    @Value("${app.admin.password}") private String adminPassword;

    private int seededAdminCount() {
        return jdbc.queryForObject("select count(*) from users where is_seeded_admin = true", Integer.class);
    }

    @Test
    @DisplayName("T12 startup created exactly one seeded admin with the ADMIN role")
    void startupCreatedOneAdmin() {
        assertThat(seededAdminCount()).isEqualTo(1);

        Map<String, Object> row = jdbc.queryForMap(
                "select username, enabled, auth_provider from users where is_seeded_admin = true");
        assertThat(row.get("username")).isEqualTo(adminUsername);
        assertThat(row.get("enabled")).isEqualTo(true);
        assertThat(row.get("auth_provider")).isEqualTo("LOCAL");

        List<String> roles = jdbc.queryForList("""
                select r.name from user_roles ur
                join roles r on r.id = ur.role_id
                join users u on u.id = ur.user_id
                where u.is_seeded_admin = true""", String.class);
        assertThat(roles).containsExactly("ADMIN");
    }

    @Test
    @DisplayName("T13 running the runner again leaves exactly one seeded admin")
    void runnerIsIdempotent() throws Exception {
        runner.run(new DefaultApplicationArguments());
        runner.run(new DefaultApplicationArguments());

        assertThat(seededAdminCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("T14 the database refuses a second seeded admin (V11 unique partial index)")
    void databaseRefusesSecondSeededAdmin() {
        assertThatThrownBy(() -> jdbc.update("""
                insert into users (username, email, password_hash, is_seeded_admin)
                values ('second-admin', 'second-admin@sakan.test', 'x', true)"""))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("T18 the stored password is a BCrypt hash, never the raw password")
    void storedPasswordIsBcrypt() {
        String stored = jdbc.queryForObject(
                "select password_hash from users where is_seeded_admin = true", String.class);

        assertThat(stored).startsWith("$2").isNotEqualTo(adminPassword);
    }
}