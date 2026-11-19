package com.sakan.support.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import javax.sql.DataSource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.flywaydb.core.Flyway;

import com.sakan.support.AbstractIntegrationTest;

/** Verifies the baseline and reference data against a clean PostGIS Testcontainer. */
class BaselineMigrationIT extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("V1 and V2 migrate a clean database and seed bilingual reference data")
    void cleanDatabaseGetsBaselineSchemaAndReferenceData() {
        Integer baselineMigrations = jdbc.queryForObject(
                "select count(*) from flyway_schema_history where version in ('1', '2') and success",
                Integer.class);
        assertThat(baselineMigrations).isEqualTo(2);

        assertThat(count("roles")).isEqualTo(5);
        assertThat(count("property_type")).isEqualTo(6);
        assertThat(count("amenity")).isEqualTo(10);
        assertThat(count("location")).isEqualTo(22);

        Map<String, Integer> locationsByType = jdbc.query(
                "select location_type, count(*) as total from location group by location_type",
                result -> {
                    Map<String, Integer> counts = new java.util.HashMap<>();
                    while (result.next()) {
                        counts.put(result.getString("location_type"), result.getInt("total"));
                    }
                    return counts;
                });
        assertThat(locationsByType)
                .containsEntry("GOVERNORATE", 3)
                .containsEntry("CITY", 6)
                .containsEntry("DISTRICT", 8)
                .containsEntry("COMPOUND", 5);

        Integer locationsMissingTranslation = jdbc.queryForObject(
                "select count(*) from location where name_en is null or name_ar is null", Integer.class);
        assertThat(locationsMissingTranslation).isZero();
    }

    @Test
    @DisplayName("Baseline and follow-on migrations install required uniqueness and browse indexes")
    void requiredConstraintsAndIndexesExist() {
        assertThat(indexExists("uq_viewing_slot_property_start")).isTrue();
        assertThat(indexExists("idx_properties_browse_composite")).isTrue();
        assertThat(indexExists("idx_property_image_active_order")).isTrue();
        assertThat(indexExists("uq_viewing_active_slot")).isTrue();
        assertThat(indexExists("idx_location_parent_type")).isTrue();
    }

    @Test
    @DisplayName("V32 adds location lookup tables and data to a database already at V31")
    void legacyV31DatabaseGetsLookupTablesAndSeeds() {
        jdbc.execute("create schema scrum23_legacy");
        jdbc.execute("create table scrum23_legacy.legacy_marker (id integer primary key)");

        Flyway.configure()
                .dataSource(dataSource)
                .schemas("scrum23_legacy")
                .baselineOnMigrate(true)
                .baselineVersion("31")
                .baselineDescription("legacy dev schema")
                .load()
                .migrate();

        assertThat(count("scrum23_legacy.property_type")).isEqualTo(6);
        assertThat(count("scrum23_legacy.amenity")).isEqualTo(10);
        assertThat(count("scrum23_legacy.location")).isEqualTo(22);
        assertThat(jdbc.queryForObject(
                "select count(*) from scrum23_legacy.flyway_schema_history where version = '32' and success",
                Integer.class)).isEqualTo(1);
    }

    private int count(String tableName) {
        return jdbc.queryForObject("select count(*) from " + tableName, Integer.class);
    }

    private boolean indexExists(String name) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "select exists (select 1 from pg_indexes where schemaname = 'public' and indexname = ?)",
                Boolean.class,
                name));
    }
}
