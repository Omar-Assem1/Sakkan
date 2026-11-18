package com.sakan.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Proves that {@link AbstractIntegrationTest} gives every subclass a real
 * PostGIS database (same image as docker-compose.yml) with Flyway migrations applied.
 */
class AbstractIntegrationTestSmokeTest extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("Runs against a real PostgreSQL server, not H2 or a local DB")
    void databaseIsPostgres() {
        String version = jdbc.queryForObject("select version()", String.class);

        assertThat(version).startsWith("PostgreSQL");
    }

    @Test
    @DisplayName("PostGIS extension is available (same image as docker-compose.yml)")
    void postgisExtensionIsAvailable() {
        Integer count = jdbc.queryForObject(
                "select count(*) from pg_available_extensions where name = 'postgis'", Integer.class);

        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("Flyway migrations ran successfully on startup")
    void flywayMigrationsApplied() {
        Integer applied = jdbc.queryForObject(
                "select count(*) from flyway_schema_history where success = true", Integer.class);

        assertThat(applied).isGreaterThan(0);
    }
}
