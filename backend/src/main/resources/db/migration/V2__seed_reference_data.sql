-- SCRUM-23: bilingual reference data for location dropdowns and listings.

INSERT INTO roles (name) VALUES
    ('BUYER_TENANT'), ('OWNER'), ('AGENT'), ('MAINTENANCE_STAFF'), ('ADMIN')
ON CONFLICT (name) DO NOTHING;

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
