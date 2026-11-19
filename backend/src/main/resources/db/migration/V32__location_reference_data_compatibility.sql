-- SCRUM-23 compatibility for databases that already applied the temporary
-- V10/V20/V30/V31 feature migrations before the V1 baseline was introduced.
-- Fresh databases already have these tables and rows from V1/V2.

CREATE TABLE IF NOT EXISTS location (
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

CREATE UNIQUE INDEX IF NOT EXISTS ux_location_root_name_en ON location (name_en) WHERE parent_id IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS ux_location_child_name_en_parent ON location (parent_id, name_en) WHERE parent_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_location_parent_type ON location (parent_id, location_type, id);

CREATE TABLE IF NOT EXISTS amenity (
    id BIGSERIAL PRIMARY KEY,
    name_en VARCHAR(80) NOT NULL UNIQUE,
    name_ar VARCHAR(80) NOT NULL
);

CREATE TABLE IF NOT EXISTS property_type (
    id BIGSERIAL PRIMARY KEY,
    name_en VARCHAR(80) NOT NULL UNIQUE,
    name_ar VARCHAR(80) NOT NULL
);

INSERT INTO property_type (name_en, name_ar) VALUES
    ('Apartment', 'شقة'),
    ('Villa', 'فيلا'),
    ('Townhouse', 'تاون هاوس'),
    ('Duplex', 'دوبلكس'),
    ('Studio', 'استوديو'),
    ('Office', 'مكتب')
ON CONFLICT (name_en) DO NOTHING;

INSERT INTO amenity (name_en, name_ar) VALUES
    ('Air conditioning', 'تكييف'),
    ('Balcony', 'شرفة'),
    ('Elevator', 'مصعد'),
    ('Parking', 'موقف سيارات'),
    ('Security', 'أمن'),
    ('Swimming pool', 'حمام سباحة'),
    ('Garden', 'حديقة'),
    ('Central heating', 'تدفئة مركزية'),
    ('Furnished', 'مفروش'),
    ('Pet friendly', 'يسمح بالحيوانات الأليفة')
ON CONFLICT (name_en) DO NOTHING;

INSERT INTO location (parent_id, location_type, name_en, name_ar) VALUES
    (NULL, 'GOVERNORATE', 'Cairo', 'القاهرة'),
    (NULL, 'GOVERNORATE', 'Alexandria', 'الإسكندرية'),
    (NULL, 'GOVERNORATE', 'Giza', 'الجيزة')
ON CONFLICT DO NOTHING;

INSERT INTO location (parent_id, location_type, name_en, name_ar)
SELECT parent.id, child.location_type, child.name_en, child.name_ar
FROM (VALUES
    ('Cairo', 'CITY', 'New Cairo', 'القاهرة الجديدة'),
    ('Cairo', 'CITY', 'Nasr City', 'مدينة نصر'),
    ('Cairo', 'CITY', 'Heliopolis', 'مصر الجديدة'),
    ('Alexandria', 'CITY', 'Alexandria City', 'مدينة الإسكندرية'),
    ('Giza', 'CITY', '6th of October City', 'مدينة السادس من أكتوبر'),
    ('Giza', 'CITY', 'Sheikh Zayed City', 'مدينة الشيخ زايد')
) AS child(parent_name, location_type, name_en, name_ar)
JOIN location parent ON parent.name_en = child.parent_name AND parent.parent_id IS NULL
ON CONFLICT DO NOTHING;

INSERT INTO location (parent_id, location_type, name_en, name_ar)
SELECT parent.id, child.location_type, child.name_en, child.name_ar
FROM (VALUES
    ('New Cairo', 'DISTRICT', 'Fifth Settlement', 'التجمع الخامس'),
    ('New Cairo', 'DISTRICT', 'First Settlement', 'التجمع الأول'),
    ('Nasr City', 'DISTRICT', 'District 1', 'الحي الأول'),
    ('Nasr City', 'DISTRICT', 'District 7', 'الحي السابع'),
    ('Alexandria City', 'DISTRICT', 'Sidi Gaber', 'سيدي جابر'),
    ('Alexandria City', 'DISTRICT', 'Montaza', 'المنتزه'),
    ('6th of October City', 'DISTRICT', 'Central District', 'الحي المركزي'),
    ('Sheikh Zayed City', 'DISTRICT', 'District 1', 'الحي الأول')
) AS child(parent_name, location_type, name_en, name_ar)
JOIN location parent ON parent.name_en = child.parent_name
    AND parent.location_type = 'CITY'
ON CONFLICT DO NOTHING;

INSERT INTO location (parent_id, location_type, name_en, name_ar)
SELECT parent.id, child.location_type, child.name_en, child.name_ar
FROM (VALUES
    ('Fifth Settlement', 'New Cairo', 'COMPOUND', 'Madinaty', 'مدينتي'),
    ('Fifth Settlement', 'New Cairo', 'COMPOUND', 'Al Rehab', 'الرحاب'),
    ('Fifth Settlement', 'New Cairo', 'COMPOUND', 'Mountain View iCity', 'ماونتن فيو آي سيتي'),
    ('Central District', '6th of October City', 'COMPOUND', 'Palm Hills October', 'بالم هيلز أكتوبر'),
    ('District 1', 'Sheikh Zayed City', 'COMPOUND', 'Zed West', 'زيد ويست')
) AS child(parent_name, city_name, location_type, name_en, name_ar)
JOIN location city ON city.name_en = child.city_name
    AND city.location_type = 'CITY'
JOIN location parent ON parent.name_en = child.parent_name
    AND parent.location_type = 'DISTRICT'
    AND parent.parent_id = city.id
ON CONFLICT DO NOTHING;
