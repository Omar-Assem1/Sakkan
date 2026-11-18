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

CREATE EXTENSION IF NOT EXISTS pg_trgm;

ALTER TABLE property
ADD COLUMN IF NOT EXISTS search_vector tsvector
GENERATED ALWAYS AS (
    setweight(to_tsvector('simple', coalesce(title, '')), 'A') ||
    setweight(to_tsvector('simple', coalesce(description, '')), 'B')
) STORED;

CREATE INDEX IF NOT EXISTS idx_property_search_vector
ON property USING GIN (search_vector);

CREATE INDEX IF NOT EXISTS idx_property_title_trgm
ON property USING GIN (title gin_trgm_ops);
