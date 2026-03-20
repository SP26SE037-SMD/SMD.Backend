ALTER TABLE subjects ALTER COLUMN student_tasks TYPE text USING student_tasks::text;
ALTER TABLE subjects ALTER COLUMN time_allocation TYPE text USING time_allocation::text;

ALTER TABLE curriculum ADD description text NULL;

DROP TABLE elective_subject;
DROP TABLE elective;


