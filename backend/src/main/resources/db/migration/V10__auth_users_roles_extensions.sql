-- SCRUM-52: idempotent so it works with or without M4's baseline (columns must match README data model)
CREATE TABLE IF NOT EXISTS roles (
                                     id   BIGSERIAL PRIMARY KEY,
                                     name VARCHAR(30) NOT NULL UNIQUE
    );

CREATE TABLE IF NOT EXISTS users (
                                     id            BIGSERIAL PRIMARY KEY,
                                     username      VARCHAR(50)  NOT NULL UNIQUE,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    phone         VARCHAR(20),
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
    );

ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;
ALTER TABLE users ADD COLUMN IF NOT EXISTS auth_provider   VARCHAR(20) NOT NULL DEFAULT 'LOCAL';
ALTER TABLE users ADD COLUMN IF NOT EXISTS google_sub      VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS is_seeded_admin BOOLEAN     NOT NULL DEFAULT FALSE;
CREATE UNIQUE INDEX IF NOT EXISTS ux_users_google_sub ON users (google_sub);

CREATE TABLE IF NOT EXISTS user_roles (
                                          user_id    BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_id    BIGINT      NOT NULL REFERENCES roles (id),
    granted_by BIGINT      REFERENCES users (id) ON DELETE SET NULL,
    granted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, role_id)
    );

INSERT INTO roles (name) VALUES
                             ('BUYER_TENANT'), ('OWNER'), ('AGENT'), ('MAINTENANCE_STAFF'), ('ADMIN')
    ON CONFLICT (name) DO NOTHING;