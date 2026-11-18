CREATE TABLE IF NOT EXISTS property (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    agent_id BIGINT,
    title VARCHAR(150) NOT NULL,
    description VARCHAR(5000),
    purpose VARCHAR(10) NOT NULL,
    property_type_id BIGINT NOT NULL,
    location_id BIGINT NOT NULL,
    price NUMERIC(14, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    bedrooms INTEGER NOT NULL,
    bathrooms INTEGER NOT NULL,
    area_sqm NUMERIC(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS property_amenity (
    property_id BIGINT NOT NULL,
    amenity_id BIGINT NOT NULL,
    PRIMARY KEY (property_id, amenity_id),
    CONSTRAINT fk_property_amenity_property FOREIGN KEY (property_id) REFERENCES property(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_properties_browse_composite 
ON property (status, location_id, property_type_id, price, bedrooms, created_at, id);

CREATE INDEX IF NOT EXISTS idx_property_amenities_composite 
ON property_amenity (amenity_id, property_id);
