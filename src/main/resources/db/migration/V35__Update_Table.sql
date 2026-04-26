ALTER TABLE regulation
    ADD COLUMN IF NOT EXISTS major_id UUID,
    ADD CONSTRAINT regulation_major_fk FOREIGN KEY (major_id) REFERENCES public.major(major_id);

ALTER TABLE regulation DROP COLUMN IF EXISTS description;
ALTER TABLE regulation DROP COLUMN IF EXISTS value;

ALTER TABLE regulation ADD COLUMN value TEXT;

ALTER TABLE subjects
    ADD COLUMN theory_periods INTEGER DEFAULT 0,
    ADD COLUMN practical_periods INTEGER DEFAULT 0,
    ADD COLUMN self_study_periods INTEGER DEFAULT 0;

