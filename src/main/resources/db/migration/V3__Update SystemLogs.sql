SET search_path TO public;

ALTER TABLE system_log
DROP COLUMN log_source,
DROP COLUMN log_level,
ADD COLUMN action VARCHAR(100) NOT NULL DEFAULT 'UNKNOWN',
ADD COLUMN target_id UUID;