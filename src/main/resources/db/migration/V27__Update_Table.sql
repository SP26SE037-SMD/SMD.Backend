ALTER TABLE syllabus
DROP COLUMN min_bloom_level;

ALTER TABLE subjects
ADD COLUMN min_bloom_level INTEGER;