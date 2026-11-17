-- Flyway Migration V20: Add composite index for high-throughput public browsing (NFR-01)
-- Covers: status = 'PUBLISHED', location hierarchy, property_type_id, price, bedrooms, created_at, and id
CREATE INDEX IF NOT EXISTS idx_properties_browse_composite 
ON property (status, location_id, property_type_id, price, bedrooms, created_at, id);

-- Index for amenity matching on property_amenity join table
CREATE INDEX IF NOT EXISTS idx_property_amenities_composite 
ON property_amenity (amenity_id, property_id);
