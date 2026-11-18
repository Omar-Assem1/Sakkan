-- SCRUM-86: EXPLAIN-based queries proving index usage

-- 1. Full-text search on title and description using GIN index:
-- Expectation: "Bitmap Index Scan on idx_property_search_vector"
EXPLAIN (ANALYZE, BUFFERS)
SELECT id, title, status
FROM property
WHERE status = 'PUBLISHED'
  AND search_vector @@ plainto_tsquery('simple', 'Smouha');

-- 2. Arabic full-text search using GIN index:
-- Expectation: "Bitmap Index Scan on idx_property_search_vector"
EXPLAIN (ANALYZE, BUFFERS)
SELECT id, title, status
FROM property
WHERE status = 'PUBLISHED'
  AND search_vector @@ plainto_tsquery('simple', 'شقة');

-- 3. Trigram fuzzy search on title using GIN trigram index:
-- Expectation: "Bitmap Index Scan on idx_property_title_trgm"
EXPLAIN (ANALYZE, BUFFERS)
SELECT id, title, status
FROM property
WHERE status = 'PUBLISHED'
  AND similarity(title, 'Aleksandria') >= 0.3;
