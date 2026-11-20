package com.sakan.demo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.sakan.support.AbstractIntegrationTest;

@Transactional
class DemoDatasetGeneratorIT extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void generatesFiveThousandPublishedListingsAndIsIdempotent() {
        DemoDatasetGenerator generator = new DemoDatasetGenerator(jdbc);

        generator.generateIfNeeded();
        generator.generateIfNeeded();

        assertThat(demoListingCount()).isEqualTo(DemoDatasetGenerator.TARGET_LISTINGS);
        assertThat(jdbc.queryForObject("""
                SELECT count(DISTINCT property_type_id)
                FROM property
                WHERE title LIKE 'Demo listing #%'
                """, Integer.class))
                .isEqualTo(jdbc.queryForObject("SELECT count(*) FROM property_type", Integer.class));
        assertThat(jdbc.queryForObject("""
                SELECT count(DISTINCT location_id)
                FROM property
                WHERE title LIKE 'Demo listing #%'
                """, Integer.class))
                .isEqualTo(jdbc.queryForObject("""
                        SELECT count(*) FROM location WHERE location_type IN ('DISTRICT', 'COMPOUND')
                        """, Integer.class));
        assertThat(jdbc.queryForObject("""
                SELECT count(*)
                FROM property
                WHERE title LIKE 'Demo listing #%'
                  AND status = 'PUBLISHED'
                """, Integer.class))
                .isEqualTo(DemoDatasetGenerator.TARGET_LISTINGS);
    }

    private int demoListingCount() {
        return jdbc.queryForObject("""
                SELECT count(*) FROM property WHERE title LIKE 'Demo listing #%'
                """, Integer.class);
    }
}
