CREATE TABLE property_image (
  id BIGSERIAL PRIMARY KEY,
  property_id BIGINT NOT NULL REFERENCES property(id) ON DELETE CASCADE,
  storage_path VARCHAR(500) NOT NULL,
  thumbnail_path VARCHAR(500) NOT NULL,
  mime_type VARCHAR(50) NOT NULL,
  file_size BIGINT NOT NULL CHECK (file_size >= 0),
  display_order INTEGER NOT NULL DEFAULT 0,
  is_primary BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  archived BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_property_image_active_order ON property_image(property_id, archived, display_order);
