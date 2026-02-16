-- =====================================================
-- Migration: Schema Update
-- Version: V2
-- Description:
-- Incremental changes after V1:
-- - Add department to subjects
-- - Add role to account
-- - Add syllabus reference to task
-- - Add new mapping tables
-- - Add new indexes
-- - Add unique constraint to vector_embeddings
-- =====================================================


-- =====================================================
-- 1️⃣ subjects: thêm department_id
-- =====================================================

ALTER TABLE subjects
ADD COLUMN department_id UUID;

ALTER TABLE subjects
ADD CONSTRAINT fk_subject_department
FOREIGN KEY (department_id)
REFERENCES department(department_id);

CREATE INDEX idx_subjects_department_id
ON subjects(department_id);


-- =====================================================
-- 2️⃣ account: thêm role_id
-- =====================================================

ALTER TABLE account
ADD COLUMN role_id UUID;

ALTER TABLE account
ADD CONSTRAINT fk_account_role
FOREIGN KEY (role_id)
REFERENCES role(role_id);

CREATE INDEX idx_account_role_id
ON account(role_id);


-- =====================================================
-- 3️⃣ task: thêm syllabus_id
-- =====================================================

ALTER TABLE task
ADD COLUMN syllabus_id UUID;

ALTER TABLE task
ADD CONSTRAINT fk_task_syllabus
FOREIGN KEY (syllabus_id)
REFERENCES syllabus(syllabus_id)
ON DELETE SET NULL;

CREATE INDEX idx_task_syllabus_id
ON task(syllabus_id);


-- =====================================================
-- 4️⃣ New Table: elective_subject
-- =====================================================

CREATE TABLE elective_subject (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    elective_id UUID NOT NULL,
    subject_id UUID NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (elective_id) REFERENCES elective(elective_id) ON DELETE CASCADE,
    FOREIGN KEY (subject_id) REFERENCES subjects(subject_id) ON DELETE CASCADE,
    UNIQUE (elective_id, subject_id)
);

CREATE INDEX idx_elective_subject_elective_id
ON elective_subject(elective_id);

CREATE INDEX idx_elective_subject_subject_id
ON elective_subject(subject_id);


-- =====================================================
-- 5️⃣ New Table: subject_prerequisite
-- =====================================================

CREATE TABLE subject_prerequisite (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subject_id UUID NOT NULL,
    prerequisite_subject_id UUID NOT NULL,
    is_mandatory BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (subject_id) REFERENCES subjects(subject_id) ON DELETE CASCADE,
    FOREIGN KEY (prerequisite_subject_id) REFERENCES subjects(subject_id) ON DELETE CASCADE,
    UNIQUE (subject_id, prerequisite_subject_id),
    CHECK (subject_id <> prerequisite_subject_id)
);

CREATE INDEX idx_subject_prerequisite_subject_id
ON subject_prerequisite(subject_id);

CREATE INDEX idx_subject_prerequisite_prereq_id
ON subject_prerequisite(prerequisite_subject_id);


-- =====================================================
-- 6️⃣ New Table: clo_session
-- =====================================================

CREATE TABLE clo_session (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clo_id UUID NOT NULL,
    session_id UUID NOT NULL,
    coverage_level VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (clo_id) REFERENCES clos(clo_id) ON DELETE CASCADE,
    FOREIGN KEY (session_id) REFERENCES session(session_id) ON DELETE CASCADE,
    UNIQUE (clo_id, session_id)
);

CREATE INDEX idx_clo_session_clo_id
ON clo_session(clo_id);

CREATE INDEX idx_clo_session_session_id
ON clo_session(session_id);


-- =====================================================
-- 7️⃣ vector_embeddings: thêm UNIQUE cho block_id
-- =====================================================

ALTER TABLE vector_embeddings
ADD CONSTRAINT uq_vector_embeddings_block UNIQUE (block_id);


-- =====================================================
-- 8️⃣ Thêm index mới cho lecturer_profile
-- =====================================================

CREATE INDEX idx_lecturer_profile_department_id
ON lecturer_profile(department_id);


-- =====================================================
-- END OF V2
-- =====================================================
