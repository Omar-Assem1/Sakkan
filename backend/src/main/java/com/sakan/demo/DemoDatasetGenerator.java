package com.sakan.demo;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Seeds repeatable property listings for local development only. */
@Component
@Profile("dev")
public class DemoDatasetGenerator {

    static final int TARGET_LISTINGS = 5_000;
    private static final Logger log = LoggerFactory.getLogger(DemoDatasetGenerator.class);
    private static final String INSERT_SQL = """
            INSERT INTO property (
                owner_id, title, description, purpose, property_type_id, location_id,
                price, currency, bedrooms, bathrooms, area_sqm, status
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PUBLISHED')
            """;

    private final JdbcTemplate jdbc;

    public DemoDatasetGenerator(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void generateOnStartup() {
        generateIfNeeded();
    }

    void generateIfNeeded() {
        long existingCount = jdbc.queryForObject(
                "SELECT count(*) FROM property WHERE title LIKE 'Demo listing #%'",
                Long.class);
        int remaining = TARGET_LISTINGS - Math.toIntExact(existingCount);
        if (remaining <= 0) {
            log.info("Demo dataset already contains at least {} listings; skipping generation.", TARGET_LISTINGS);
            return;
        }

        Long ownerId = jdbc.queryForObject("SELECT id FROM users ORDER BY id LIMIT 1", Long.class);
        List<Long> propertyTypeIds = jdbc.queryForList("SELECT id FROM property_type ORDER BY id", Long.class);
        List<Long> locationIds = jdbc.queryForList("""
                SELECT id FROM location
                WHERE location_type IN ('DISTRICT', 'COMPOUND')
                ORDER BY id
                """, Long.class);
        if (propertyTypeIds.isEmpty() || locationIds.isEmpty()) {
            throw new IllegalStateException("Demo listings require seeded property types and locations.");
        }

        long firstListingNumber = existingCount + 1;
        List<Object[]> rows = IntStream.range(0, remaining)
                .mapToObj(offset -> listingRow(firstListingNumber + offset, ownerId, propertyTypeIds, locationIds))
                .toList();
        jdbc.batchUpdate(INSERT_SQL, rows);
        log.info("Generated {} demo listings; total demo listings is now {}.", remaining, existingCount + remaining);
    }

    private static Object[] listingRow(
            long listingNumber, Long ownerId, List<Long> propertyTypeIds, List<Long> locationIds) {
        int index = Math.toIntExact(listingNumber - 1);
        int propertyTypeIndex = index % propertyTypeIds.size();
        int locationIndex = (index / propertyTypeIds.size()) % locationIds.size();
        int bedrooms = index % 6;
        long price = 500_000L + (index % 400) * 50_000L;
        long area = 30L + (index % 321);

        return new Object[] {
                ownerId,
                "Demo listing #" + listingNumber,
                "Development sample listing " + listingNumber + ".",
                index % 2 == 0 ? "SALE" : "RENT",
                propertyTypeIds.get(propertyTypeIndex),
                locationIds.get(locationIndex),
                BigDecimal.valueOf(price),
                "EGP",
                bedrooms,
                Math.max(1, (bedrooms + 1) / 2),
                BigDecimal.valueOf(area),
        };
    }
}
