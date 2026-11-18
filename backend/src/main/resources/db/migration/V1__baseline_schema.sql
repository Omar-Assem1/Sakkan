-- SCRUM-23 (SKN-8): Flyway baseline schema
-- Creates core tables: roles, users, user_roles, location, property_type, amenity,
-- property, property_amenity, viewing_slot, viewing.
-- NOTE: property_image is owned by V22 (uses CREATE TABLE without IF NOT EXISTS).
--       Cross-module IDs (owner_id, location_id, buyer_id, etc.) use BIGINT to stay decoupled
--       and match V20, V30, and V31 exactly.

CREATE TABLE IF NOT EXISTS roles (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(30) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS users (
    id              BIGSERIAL    PRIMARY KEY,
    username        VARCHAR(50)  NOT NULL UNIQUE,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255),
    phone           VARCHAR(20),
    auth_provider   VARCHAR(20)  NOT NULL DEFAULT 'LOCAL'
        CONSTRAINT chk_users_auth_provider CHECK (auth_provider IN ('LOCAL', 'GOOGLE')),
    google_sub      VARCHAR(255) UNIQUE,
    is_seeded_admin BOOLEAN      NOT NULL DEFAULT FALSE,
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id    BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_id    BIGINT      NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    granted_by BIGINT      REFERENCES users (id) ON DELETE SET NULL,
    granted_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE IF NOT EXISTS location (
    id         BIGSERIAL    PRIMARY KEY,
    parent_id  BIGINT       REFERENCES location (id) ON DELETE SET NULL,
    name_en    VARCHAR(100) NOT NULL,
    name_ar    VARCHAR(100) NOT NULL,
    type       VARCHAR(20)  NOT NULL
        CONSTRAINT chk_location_type CHECK (type IN ('GOVERNORATE', 'CITY', 'DISTRICT', 'COMPOUND')),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS property_type (
    id         BIGSERIAL    PRIMARY KEY,
    code       VARCHAR(50)  NOT NULL UNIQUE,
    name_en    VARCHAR(100) NOT NULL,
    name_ar    VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS amenity (
    id         BIGSERIAL    PRIMARY KEY,
    name_en    VARCHAR(100) NOT NULL,
    name_ar    VARCHAR(100) NOT NULL,
    icon       VARCHAR(100),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS property (
    id               BIGSERIAL                PRIMARY KEY,
    owner_id         BIGINT                   NOT NULL,
    agent_id         BIGINT,
    title            VARCHAR(150)             NOT NULL,
    description      VARCHAR(5000),
    purpose          VARCHAR(10)              NOT NULL
        CONSTRAINT chk_property_purpose CHECK (purpose IN ('SALE', 'RENT')),
    property_type_id BIGINT                   NOT NULL,
    location_id      BIGINT                   NOT NULL,
    price            NUMERIC(14, 2)           NOT NULL
        CONSTRAINT chk_property_price_positive CHECK (price > 0),
    currency         VARCHAR(3)               NOT NULL
        CONSTRAINT chk_property_currency CHECK (currency IN ('EGP', 'USD')),
    bedrooms         INTEGER                  NOT NULL DEFAULT 0
        CONSTRAINT chk_property_bedrooms CHECK (bedrooms >= 0),
    bathrooms        INTEGER                  NOT NULL DEFAULT 0
        CONSTRAINT chk_property_bathrooms CHECK (bathrooms >= 0),
    area_sqm         NUMERIC(10, 2)           NOT NULL
        CONSTRAINT chk_property_area_positive CHECK (area_sqm > 0),
    status           VARCHAR(20)              NOT NULL DEFAULT 'DRAFT'
        CONSTRAINT chk_property_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'RENTED', 'SOLD', 'ARCHIVED')),
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS property_amenity (
    property_id BIGINT NOT NULL REFERENCES property (id) ON DELETE CASCADE,
    amenity_id  BIGINT NOT NULL,
    PRIMARY KEY (property_id, amenity_id)
);

-- property_image is intentionally NOT created here.
-- V22__create_property_images.sql owns this table with its full definition.

CREATE TABLE IF NOT EXISTS viewing_slot (
    id          BIGSERIAL                PRIMARY KEY,
    property_id BIGINT                   NOT NULL REFERENCES property (id) ON DELETE CASCADE,
    created_by  BIGINT                   NOT NULL,
    start_time  TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time    TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- viewing columns and constraints match V31__create_viewing_table.sql exactly.
CREATE TABLE IF NOT EXISTS viewing (
    id          BIGSERIAL                PRIMARY KEY,
    slot_id     BIGINT                   NOT NULL,
    property_id BIGINT                   NOT NULL,
    buyer_id    BIGINT                   NOT NULL,
    status      VARCHAR(32)              NOT NULL,
    notes       TEXT,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_viewing_slot     FOREIGN KEY (slot_id)     REFERENCES viewing_slot (id) ON DELETE CASCADE,
    CONSTRAINT fk_viewing_property FOREIGN KEY (property_id) REFERENCES property (id)      ON DELETE CASCADE,
    CONSTRAINT chk_viewing_status  CHECK (status IN ('REQUESTED', 'CONFIRMED', 'CANCELLED', 'COMPLETED'))
);
