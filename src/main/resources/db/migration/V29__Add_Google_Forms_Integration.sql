CREATE TABLE google_form_records (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    curriculum_id uuid NOT NULL REFERENCES curriculum(curriculum_id),
    google_form_id varchar(200),
    form_url text,
    edit_url text,
    form_type varchar(100),
    is_active boolean DEFAULT false,
    created_at timestamp DEFAULT now()
);

CREATE TABLE feedback_form_sections (
    section_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    form_record_id uuid NOT NULL REFERENCES google_form_records(id),
    title varchar(200),
    order_index integer,
    after_section_action varchar(20) DEFAULT 'NEXT',
    target_section_id uuid
);

CREATE TABLE feedback_form_questions (
    question_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    section_id uuid NOT NULL REFERENCES feedback_form_sections(section_id),
    content text NOT NULL,
    type varchar(30),
    is_required boolean DEFAULT false,
    order_index integer,
    google_item_id varchar(50)
);

CREATE TABLE feedback_form_options (
    option_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id uuid NOT NULL REFERENCES feedback_form_questions(question_id),
    option_text text,
    order_index integer,
    next_section_id uuid
);

CREATE TABLE form_question_mapping (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    form_record_id uuid NOT NULL REFERENCES google_form_records(id),
    question_id uuid NOT NULL,
    google_item_id varchar(50) NOT NULL,
    backend_section_id varchar(100)
);

INSERT INTO permission (permission_id, permission_name, description)
SELECT gen_random_uuid(), 'FEEDBACK_VIEW', 'Xem feedback va bao cao'
WHERE NOT EXISTS (
    SELECT 1 FROM permission p WHERE p.permission_name = 'FEEDBACK_VIEW'
);

INSERT INTO permission (permission_id, permission_name, description)
SELECT gen_random_uuid(), 'FEEDBACK_MANAGE', 'Tao sua form, trigger build, quan ly cau hoi'
WHERE NOT EXISTS (
    SELECT 1 FROM permission p WHERE p.permission_name = 'FEEDBACK_MANAGE'
);

INSERT INTO role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM role r
JOIN permission p ON p.permission_name IN ('FEEDBACK_VIEW', 'FEEDBACK_MANAGE')
WHERE r.role_name IN ('HOCFDC', 'HOPDC')
AND NOT EXISTS (
    SELECT 1
    FROM role_permission rp
    WHERE rp.role_id = r.role_id
      AND rp.permission_id = p.permission_id
);