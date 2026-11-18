-- SCRUM-23 (SKN-8): Flyway reference seed data
-- Seeds reference data: roles, property types, amenities, and bilingual Egyptian location hierarchy

-- 1. Roles
INSERT INTO roles (name) VALUES
    ('BUYER_TENANT'),
    ('OWNER'),
    ('AGENT'),
    ('MAINTENANCE_STAFF'),
    ('ADMIN')
ON CONFLICT (name) DO NOTHING;

-- 2. Property Types
INSERT INTO property_type (code, name_en, name_ar) VALUES
    ('APARTMENT', 'Apartment', 'شقة'),
    ('VILLA', 'Villa', 'فيلا'),
    ('TOWNHOUSE', 'Townhouse', 'تاون هاوس'),
    ('TWIN_HOUSE', 'Twin House', 'توين هاوس'),
    ('DUPLEX', 'Duplex', 'دوبلكس'),
    ('PENTHOUSE', 'Penthouse', 'بنتهاوس'),
    ('CHALET', 'Chalet', 'شاليه'),
    ('STUDIO', 'Studio', 'ستوديو')
ON CONFLICT (code) DO NOTHING;

-- 3. Amenities
INSERT INTO amenity (name_en, name_ar, icon) VALUES
    ('Balcony', 'شرفة', 'balcony'),
    ('Central AC', 'تكييف مركزي', 'air-conditioner'),
    ('Security', 'أمن وحراسة', 'shield-check'),
    ('Swimming Pool', 'حمام سباحة', 'swimming-pool'),
    ('Gym', 'صالة ألعاب رياضية', 'dumbbell'),
    ('Covered Parking', 'موقف سيارات مغطى', 'car'),
    ('Elevator', 'مصعد', 'elevator'),
    ('Pets Allowed', 'يسمح بالحيوانات الأليفة', 'paw'),
    ('Private Garden', 'حديقة خاصة', 'tree'),
    ('Kitchen Appliances', 'أجهزة المطبخ', 'chef-hat'),
    ('Maid Room', 'غرفة خادمة', 'bed'),
    ('Built-in Wardrobes', 'خزائن مدمجة', 'wardrobe')
ON CONFLICT DO NOTHING;

-- 4. Location Hierarchy (Egypt: Governorate -> City -> District -> Compound)

-- Governorates (Level 1)
INSERT INTO location (id, parent_id, name_en, name_ar, type) VALUES
    (1, NULL, 'Cairo', 'القاهرة', 'GOVERNORATE'),
    (2, NULL, 'Giza', 'الجيزة', 'GOVERNORATE'),
    (3, NULL, 'Alexandria', 'الإسكندرية', 'GOVERNORATE')
ON CONFLICT (id) DO NOTHING;

-- Cities / Major Areas under Cairo (Level 2)
INSERT INTO location (id, parent_id, name_en, name_ar, type) VALUES
    (10, 1, 'New Cairo', 'القاهرة الجديدة', 'CITY'),
    (11, 1, 'Nasr City', 'مدينة نصر', 'CITY'),
    (12, 1, 'Maadi', 'المعادي', 'CITY'),
    (13, 1, 'Zamalek', 'الزمالك', 'CITY'),
    (14, 1, 'Heliopolis', 'مصر الجديدة', 'CITY')
ON CONFLICT (id) DO NOTHING;

-- Districts & Compounds under New Cairo (Level 3 & 4)
INSERT INTO location (id, parent_id, name_en, name_ar, type) VALUES
    (100, 10, 'Fifth Settlement', 'التجمع الخامس', 'DISTRICT'),
    (101, 10, 'First Settlement', 'التجمع الأول', 'DISTRICT'),
    (102, 10, 'Rehab City', 'مدينة الرحاب', 'COMPOUND'),
    (103, 10, 'Madinaty', 'مدينتي', 'COMPOUND')
ON CONFLICT (id) DO NOTHING;

-- Cities / Major Areas under Giza (Level 2)
INSERT INTO location (id, parent_id, name_en, name_ar, type) VALUES
    (20, 2, '6th of October', 'السادس من أكتوبر', 'CITY'),
    (21, 2, 'Sheikh Zayed', 'الشيخ زايد', 'CITY'),
    (22, 2, 'Dokki', 'الدقي', 'CITY'),
    (23, 2, 'Mohandessin', 'المهندسين', 'CITY')
ON CONFLICT (id) DO NOTHING;

-- Districts & Compounds under Sheikh Zayed & 6th of October (Level 3 & 4)
INSERT INTO location (id, parent_id, name_en, name_ar, type) VALUES
    (200, 21, 'Beverly Hills', 'بيفرلي هيلز', 'COMPOUND'),
    (201, 21, 'Westown', 'ويست تاون', 'COMPOUND'),
    (202, 20, 'Northern Expansions', 'التوسعات الشمالية', 'DISTRICT')
ON CONFLICT (id) DO NOTHING;

-- Cities / Major Areas under Alexandria (Level 2)
INSERT INTO location (id, parent_id, name_en, name_ar, type) VALUES
    (30, 3, 'Smouha', 'سموحة', 'CITY'),
    (31, 3, 'Sidi Gaber', 'سيدي جابر', 'CITY'),
    (32, 3, 'Gleem', 'جليم', 'CITY'),
    (33, 3, 'Montaza', 'المنتزه', 'CITY'),
    (34, 3, 'North Coast', 'الساحل الشمالي', 'CITY')
ON CONFLICT (id) DO NOTHING;

-- Reset identity sequence for location so future inserts autoincrement cleanly past manual IDs
SELECT setval(pg_get_serial_sequence('location', 'id'), coalesce(max(id), 1), true) FROM location;
