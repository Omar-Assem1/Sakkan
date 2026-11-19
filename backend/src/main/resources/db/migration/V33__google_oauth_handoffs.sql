-- SCRUM-56: short-lived one-time codes used to hand Google identity to the Sakan JWT API.
CREATE TABLE IF NOT EXISTS google_login_handoffs (
    code_hash      CHAR(64) PRIMARY KEY,
    google_sub     VARCHAR(255) NOT NULL,
    email          VARCHAR(255) NOT NULL,
    email_verified BOOLEAN NOT NULL,
    hosted_domain  VARCHAR(255),
    expires_at     TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_google_login_handoffs_expiry ON google_login_handoffs (expires_at);
