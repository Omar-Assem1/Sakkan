-- Flyway Migration V21: Full-text search and trigram indexes (SKN-23 / SCRUM-86)
-- Assignee: Member 2 (Version range V20-V29)
-- Purpose: Enable pg_trgm, add generated search_vector tsvector column, GIN index on search_vector, and GIN trigram index on title.

-- 1. Ensure property table exists if running on fresh database
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

-- 2. Enable pg_trgm extension for fuzzy trigram matching and typo tolerance
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- 3. Add generated tsvector column covering title (weight A) and description (weight B)
-- Uses 'simple' configuration to preserve exact character tokens across Arabic and English without aggressive stemming
ALTER TABLE property
ADD COLUMN IF NOT EXISTS search_vector tsvector
GENERATED ALWAYS AS (
    setweight(to_tsvector('simple', coalesce(title, '')), 'A') ||
    setweight(to_tsvector('simple', coalesce(description, '')), 'B')
) STORED;

-- 4. Create GIN index on search_vector for sub-millisecond full-text queries
CREATE INDEX IF NOT EXISTS idx_property_search_vector
ON property USING GIN (search_vector);

-- 5. Create GIN trigram index on title for typo tolerance and similarity queries
CREATE INDEX IF NOT EXISTS idx_property_title_trgm
ON property USING GIN (title gin_trgm_ops);
