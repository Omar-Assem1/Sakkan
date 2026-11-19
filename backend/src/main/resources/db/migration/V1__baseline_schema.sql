-- SCRUM-23: foundational schema. Later feature migrations add indexes and
-- extensions; core tables live here so a fresh database starts in a usable state.

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    phone VARCHAR(20),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    auth_provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL',
    google_sub VARCHAR(255),
    is_seeded_admin BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_users_auth_provider CHECK (auth_provider IN ('LOCAL', 'GOOGLE'))
);

CREATE UNIQUE INDEX ux_users_google_sub ON users (google_sub) WHERE google_sub IS NOT NULL;

CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(30) NOT NULL UNIQUE
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles (id),
    granted_by BIGINT REFERENCES users (id) ON DELETE SET NULL,
    granted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE location (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT REFERENCES location (id) ON DELETE RESTRICT,
    location_type VARCHAR(20) NOT NULL,
    name_en VARCHAR(120) NOT NULL,
    name_ar VARCHAR(120) NOT NULL,
    CONSTRAINT chk_location_type CHECK (location_type IN ('GOVERNORATE', 'CITY', 'DISTRICT', 'COMPOUND')),
    CONSTRAINT chk_location_root_type CHECK (
        (parent_id IS NULL AND location_type = 'GOVERNORATE') OR
        (parent_id IS NOT NULL AND location_type <> 'GOVERNORATE')
    )
);

CREATE UNIQUE INDEX ux_location_root_name_en ON location (name_en) WHERE parent_id IS NULL;
CREATE UNIQUE INDEX ux_location_child_name_en_parent ON location (parent_id, name_en) WHERE parent_id IS NOT NULL;
CREATE INDEX idx_location_parent_type ON location (parent_id, location_type, id);

CREATE TABLE amenity (
    id BIGSERIAL PRIMARY KEY,
    name_en VARCHAR(80) NOT NULL UNIQUE,
    name_ar VARCHAR(80) NOT NULL
);

CREATE TABLE property_type (
    id BIGSERIAL PRIMARY KEY,
    name_en VARCHAR(80) NOT NULL UNIQUE,
    name_ar VARCHAR(80) NOT NULL
);

CREATE TABLE property (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL REFERENCES users (id),
    agent_id BIGINT REFERENCES users (id),
    title VARCHAR(150) NOT NULL,
    description VARCHAR(5000),
    purpose VARCHAR(10) NOT NULL,
    property_type_id BIGINT NOT NULL REFERENCES property_type (id),
    location_id BIGINT NOT NULL REFERENCES location (id),
    price NUMERIC(14, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    bedrooms INTEGER NOT NULL,
    bathrooms INTEGER NOT NULL,
    area_sqm NUMERIC(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_property_purpose CHECK (purpose IN ('SALE', 'RENT')),
    CONSTRAINT chk_property_currency CHECK (currency IN ('EGP', 'USD')),
    CONSTRAINT chk_property_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'RENTED', 'SOLD', 'ARCHIVED')),
    CONSTRAINT chk_property_price_nonnegative CHECK (price >= 0),
    CONSTRAINT chk_property_bedrooms_nonnegative CHECK (bedrooms >= 0),
    CONSTRAINT chk_property_bathrooms_nonnegative CHECK (bathrooms >= 0),
    CONSTRAINT chk_property_area_positive CHECK (area_sqm > 0)
);

CREATE TABLE property_amenity (
    property_id BIGINT NOT NULL REFERENCES property (id) ON DELETE CASCADE,
    amenity_id BIGINT NOT NULL REFERENCES amenity (id),
    PRIMARY KEY (property_id, amenity_id)
);

CREATE TABLE viewing_slot (
    id BIGSERIAL PRIMARY KEY,
    property_id BIGINT NOT NULL REFERENCES property (id) ON DELETE CASCADE,
    created_by BIGINT NOT NULL REFERENCES users (id),
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_viewing_slot_time_range CHECK (end_time > start_time),
    CONSTRAINT uq_viewing_slot_property_start UNIQUE (property_id, start_time)
);

CREATE TABLE viewing (
    id BIGSERIAL PRIMARY KEY,
    slot_id BIGINT NOT NULL REFERENCES viewing_slot (id) ON DELETE CASCADE,
    property_id BIGINT NOT NULL REFERENCES property (id) ON DELETE CASCADE,
    buyer_id BIGINT NOT NULL REFERENCES users (id),
    status VARCHAR(32) NOT NULL,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_viewing_status CHECK (status IN ('REQUESTED', 'CONFIRMED', 'CANCELLED', 'COMPLETED'))
);
