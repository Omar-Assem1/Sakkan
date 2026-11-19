-- SCRUM-61: at most one seeded admin, enforced by the database (two instances starting together cannot create two).
CREATE UNIQUE INDEX IF NOT EXISTS ux_users_single_seeded_admin
    ON users (is_seeded_admin)
    WHERE is_seeded_admin;