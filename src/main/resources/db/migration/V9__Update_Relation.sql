ALTER TABLE subjects ALTER COLUMN student_tasks TYPE text USING student_tasks::text;
ALTER TABLE subjects ALTER COLUMN time_allocation TYPE text USING time_allocation::text;

ALTER TABLE curriculum ADD description text NULL;

ALTER TABLE material ADD syllabus_id uuid NOT NULL;
ALTER TABLE material ADD CONSTRAINT material_syllabus_fk FOREIGN KEY (syllabus_id) REFERENCES syllabus(syllabus_id);
ALTER TABLE material DROP CONSTRAINT material_uploaded_by_fkey;
ALTER TABLE material DROP COLUMN uploaded_by;

-- 1. Xóa cột file_path
ALTER TABLE material DROP COLUMN IF EXISTS file_path;

-- 2. Xóa cột file_size
ALTER TABLE material DROP COLUMN IF EXISTS file_size;

DROP TABLE elective_subject;
DROP TABLE elective;


