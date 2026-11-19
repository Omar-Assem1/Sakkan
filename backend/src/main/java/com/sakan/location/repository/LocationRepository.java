package com.sakan.location.repository;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.sakan.location.domain.LocationType;

@Repository
public class LocationRepository {

    private static final RowMapper<LocationRecord> LOCATION_ROW_MAPPER = (resultSet, rowNumber) -> {
        long parentIdValue = resultSet.getLong("parent_id");
        Long parentId = resultSet.wasNull() ? null : parentIdValue;
        return new LocationRecord(
                resultSet.getLong("id"),
                parentId,
                LocationType.valueOf(resultSet.getString("location_type")),
                resultSet.getString("name_en"),
                resultSet.getString("name_ar"));
    };

    private final JdbcTemplate jdbc;

    public LocationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<LocationRecord> findGovernorates() {
        return jdbc.query("""
                SELECT id, parent_id, location_type, name_en, name_ar
                FROM location
                WHERE parent_id IS NULL AND location_type = 'GOVERNORATE'
                ORDER BY id
                """, LOCATION_ROW_MAPPER);
    }

    public List<LocationRecord> findChildren(long parentId, LocationType parentType, LocationType childType) {
        return jdbc.query("""
                SELECT child.id, child.parent_id, child.location_type, child.name_en, child.name_ar
                FROM location child
                JOIN location parent ON parent.id = child.parent_id
                WHERE parent.id = ?
                  AND parent.location_type = ?
                  AND child.location_type = ?
                ORDER BY child.id
                """, LOCATION_ROW_MAPPER, parentId, parentType.name(), childType.name());
    }

    public List<Long> findDescendantIds(long rootId) {
        return jdbc.query("""
                WITH RECURSIVE location_tree AS (
                    SELECT id, parent_id
                    FROM location
                    WHERE id = ?
                    UNION ALL
                    SELECT child.id, child.parent_id
                    FROM location child
                    JOIN location_tree parent ON child.parent_id = parent.id
                )
                SELECT id FROM location_tree ORDER BY id
                """, (resultSet, rowNumber) -> resultSet.getLong("id"), rootId);
    }

    public record LocationRecord(
            Long id,
            Long parentId,
            LocationType type,
            String nameEn,
            String nameAr) {}
}
