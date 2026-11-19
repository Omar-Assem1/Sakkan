-- SCRUM-99 (SKN-26): viewing appointments and partial unique index for active bookings.
-- TODO(SKN-8): fold into M4's baseline schema once V1 is merged. IF NOT EXISTS keeps this safe if V1 already created it.
CREATE TABLE IF NOT EXISTS viewing (
    id          BIGSERIAL PRIMARY KEY,
    slot_id     BIGINT                   NOT NULL,
    property_id BIGINT                   NOT NULL,
    buyer_id    BIGINT                   NOT NULL,
    status      VARCHAR(32)              NOT NULL,
    notes       TEXT,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_viewing_slot FOREIGN KEY (slot_id) REFERENCES viewing_slot (id) ON DELETE CASCADE,
    CONSTRAINT fk_viewing_property FOREIGN KEY (property_id) REFERENCES property (id) ON DELETE CASCADE,
    CONSTRAINT chk_viewing_status CHECK (status IN ('REQUESTED', 'CONFIRMED', 'CANCELLED', 'COMPLETED'))
);

-- Guarantees at most one active (REQUESTED or CONFIRMED) viewing per slot while allowing re-booking after CANCELLED.
CREATE UNIQUE INDEX IF NOT EXISTS uq_viewing_active_slot
    ON viewing (slot_id)
    WHERE status IN ('REQUESTED', 'CONFIRMED');

-- Supports role-filtered "My Viewings" queries (SCRUM-102).
CREATE INDEX IF NOT EXISTS idx_viewing_buyer_status
    ON viewing (buyer_id, status);

CREATE INDEX IF NOT EXISTS idx_viewing_property_status
    ON viewing (property_id, status);
