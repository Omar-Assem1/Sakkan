-- SCRUM-23 (SKN-8): Constraints and performance indexes

-- 1. Viewing Slot time-range constraint and unique slot start (idempotent)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_viewing_slot_time_range') THEN
        ALTER TABLE viewing_slot ADD CONSTRAINT chk_viewing_slot_time_range CHECK (end_time > start_time);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_viewing_slot_property_start') THEN
        ALTER TABLE viewing_slot ADD CONSTRAINT uq_viewing_slot_property_start UNIQUE (property_id, start_time);
    END IF;
END $$;

-- NOTE: The partial unique index uq_viewing_active_slot on viewing(slot_id)
-- is owned by V31__create_viewing_table.sql (IF NOT EXISTS). Omitted here to avoid duplication.

-- 2. Composite B-tree index to support property browse and filter queries
CREATE INDEX IF NOT EXISTS idx_property_location_type_price
    ON property (location_id, property_type_id, price);

-- 3. Interval overlap check and ordered slot listing
CREATE INDEX IF NOT EXISTS idx_viewing_slot_property_time
    ON viewing_slot (property_id, start_time, end_time);

-- 4. Recursive CTE / cascading dropdown lookups on location hierarchy
CREATE INDEX IF NOT EXISTS idx_location_parent_id
    ON location (parent_id);

-- 5. FK indexes on property for owner/agent queries
CREATE INDEX IF NOT EXISTS idx_property_owner_id
    ON property (owner_id);

CREATE INDEX IF NOT EXISTS idx_property_agent_id
    ON property (agent_id);
