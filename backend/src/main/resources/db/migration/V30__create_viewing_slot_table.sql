-- SCRUM-95 (SKN-25): viewing availability slots.
-- TODO(SKN-8): fold into M4's baseline schema once V1 is merged. IF NOT EXISTS keeps this safe if V1 already created it.
CREATE TABLE IF NOT EXISTS viewing_slot (
    id          BIGSERIAL PRIMARY KEY,
    property_id BIGINT                   NOT NULL,
    created_by  BIGINT                   NOT NULL,
    start_time  TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time    TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_viewing_slot_property FOREIGN KEY (property_id) REFERENCES property (id) ON DELETE CASCADE,
    CONSTRAINT chk_viewing_slot_time_range CHECK (end_time > start_time),
    CONSTRAINT uq_viewing_slot_property_start UNIQUE (property_id, start_time)
);

-- Supports the overlap check (property_id = ? AND start_time < ? AND end_time > ?) and ordered listing.
CREATE INDEX IF NOT EXISTS idx_viewing_slot_property_time
    ON viewing_slot (property_id, start_time, end_time);
