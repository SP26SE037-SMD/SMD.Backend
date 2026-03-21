DROP TABLE sprint_member;

ALTER TABLE task ADD task_type varchar(50) NULL;

ALTER TABLE review_task RENAME TO request;
ALTER TABLE request RENAME COLUMN review_id TO request_id;
ALTER TABLE request RENAME COLUMN title_task TO title;
ALTER TABLE request ADD requester_id uuid NOT NULL;
ALTER TABLE request ADD CONSTRAINT request_account_fk FOREIGN KEY (requester_id) REFERENCES account(account_id);


ALTER TABLE account ADD department_id uuid NULL;
ALTER TABLE account ADD CONSTRAINT account_department_fk FOREIGN KEY (department_id) REFERENCES department(department_id);

ALTER TABLE subjects ADD tool varchar(150) NULL;

ALTER TABLE subjects ALTER COLUMN student_tasks TYPE text USING student_tasks::text;
ALTER TABLE subjects ALTER COLUMN time_allocation TYPE text USING time_allocation::text;

ALTER TABLE curriculum ADD description text NULL;

ALTER TABLE material ADD syllabus_id uuid NOT NULL;
ALTER TABLE material ADD CONSTRAINT material_syllabus_fk FOREIGN KEY (syllabus_id) REFERENCES syllabus(syllabus_id);
ALTER TABLE material DROP CONSTRAINT material_uploaded_by_fkey;
ALTER TABLE material DROP COLUMN uploaded_by;


DROP TABLE elective_subject;
DROP TABLE elective;

DROP TABLE public.assessment_syllabus;

ALTER TABLE assessments ADD syllabus_id uuid NULL;
ALTER TABLE assessments ADD CONSTRAINT assessments_syllabus_fk FOREIGN KEY (syllabus_id) REFERENCES syllabus(syllabus_id);

ALTER TABLE assessment_templates DROP CONSTRAINT assessment_templates_type_id_fkey;
ALTER TABLE assessment_templates DROP COLUMN type_id;
ALTER TABLE assessment_templates DROP COLUMN default_weight;
ALTER TABLE assessment_templates DROP COLUMN default_duration;



-- 1. Xóa cột file_path
ALTER TABLE material DROP COLUMN IF EXISTS file_path;

-- 2. Xóa cột file_size
ALTER TABLE material DROP COLUMN IF EXISTS file_size;

-- 1. Xóa các cột không còn sử dụng
ALTER TABLE blocks DROP COLUMN IF EXISTS block_sequence;
ALTER TABLE blocks DROP COLUMN IF EXISTS page_number;

-- 2. Thêm cột idx để quản lý thứ tự (Kiểu Integer)
ALTER TABLE blocks ADD COLUMN idx INT4 DEFAULT 0;

-- 3. Thêm ràng buộc NOT NULL cho idx nếu cần thiết
ALTER TABLE blocks ALTER COLUMN idx SET NOT NULL;

ALTER TABLE blocks ADD COLUMN IF NOT EXISTS block_style TEXT;
