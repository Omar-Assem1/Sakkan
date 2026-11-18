package com.sakan.migration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.sakan.support.AbstractIntegrationTest;
import com.sakan.support.tags.RegressionTest;

/**
 * SCRUM-23 (SKN-8): Integration tests for Flyway baseline schema and seed data.
 * Verifies that all core tables exist, reference data seeds correctly,
 * and critical constraints/indexes (like double-booking prevention) are active.
 */
@Transactional
@RegressionTest
class FlywayBaselineMigrationIT extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("All baseline tables exist in schema public")
    void baselineTablesExist() {
        String[] expectedTables = {
                "roles", "users", "user_roles", "location", "property_type",
                "amenity", "property", "property_amenity", "property_image",
                "viewing_slot", "viewing"
        };

        for (String table : expectedTables) {
            Integer count = jdbc.queryForObject(
                    "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = ?",
                    Integer.class, table);
            assertThat(count)
                    .as("Table %s should exist in schema public", table)
                    .isEqualTo(1);
        }
    }

    @Test
    @DisplayName("Roles seed count is 5")
    void rolesSeedCount() {
        Integer count = jdbc.queryForObject("SELECT count(*) FROM roles", Integer.class);
        assertThat(count).isEqualTo(5);
    }

    @Test
    @DisplayName("Property types seed count is 8")
    void propertyTypesSeedCount() {
        Integer count = jdbc.queryForObject("SELECT count(*) FROM property_type", Integer.class);
        assertThat(count).isEqualTo(8);
    }

    @Test
    @DisplayName("Amenities seed count is 12")
    void amenitiesSeedCount() {
        Integer count = jdbc.queryForObject("SELECT count(*) FROM amenity", Integer.class);
        assertThat(count).isEqualTo(12);
    }

    @Test
    @DisplayName("Location hierarchy is seeded with governorates, cities, districts, and compounds")
    void locationHierarchySeeded() {
        Integer totalLocations = jdbc.queryForObject("SELECT count(*) FROM location", Integer.class);
        assertThat(totalLocations).isGreaterThanOrEqualTo(15);

        Integer governorates = jdbc.queryForObject(
                "SELECT count(*) FROM location WHERE parent_id IS NULL AND type = 'GOVERNORATE'", Integer.class);
        assertThat(governorates).isEqualTo(3);
    }

    @Test
    @DisplayName("Double-booking partial unique index exists on viewing(slot_id)")
    void doubleBookingIndexExists() {
        Integer indexCount = jdbc.queryForObject(
                "SELECT count(*) FROM pg_indexes WHERE tablename = 'viewing' AND indexname = 'uq_viewing_active_slot'",
                Integer.class);
        assertThat(indexCount).isEqualTo(1);
    }

    @Test
    @DisplayName("Composite browse index on property(location_id, property_type_id, price) exists")
    void compositeBrowseIndexExists() {
        Integer indexCount = jdbc.queryForObject(
                "SELECT count(*) FROM pg_indexes WHERE tablename = 'property' AND indexname = 'idx_property_location_type_price'",
                Integer.class);
        assertThat(indexCount).isEqualTo(1);
    }
}
