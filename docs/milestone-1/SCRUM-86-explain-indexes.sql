EXPLAIN (ANALYZE, BUFFERS)
SELECT id, title, status
FROM property
WHERE status = 'PUBLISHED'
  AND search_vector @@ plainto_tsquery('simple', 'Smouha');

EXPLAIN (ANALYZE, BUFFERS)
SELECT id, title, status
FROM property
WHERE status = 'PUBLISHED'
  AND search_vector @@ plainto_tsquery('simple', 'شقة');

EXPLAIN (ANALYZE, BUFFERS)
SELECT id, title, status
FROM property
WHERE status = 'PUBLISHED'
  AND similarity(title, 'Aleksandria') >= 0.3;
