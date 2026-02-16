-- =====================================================
-- Migration: Initial Schema
-- Version: V1
-- Created: 2026-02-15
-- =====================================================
-- Description:
-- Initial database schema for SMD (Syllabus Management & Development)
-- This creates all tables according to the Entity-Relationship design
-- following OBE (Outcome-Based Education) principles
-- =====================================================

-- =====================================================
-- KHỐI 1: KHUNG CHƯƠNG TRÌNH (ACADEMIC STRUCTURE)
-- =====================================================

-- Bảng: Department (Khoa/Bộ môn)
CREATE TABLE IF NOT EXISTS department (
    department_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    department_code VARCHAR(20) UNIQUE NOT NULL,
    department_name VARCHAR(100) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Bảng: Major (Ngành học)
CREATE TABLE IF NOT EXISTS major (
    major_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    major_code VARCHAR(20) UNIQUE NOT NULL,
    major_name VARCHAR(100) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Bảng: Curriculum (Khung chương trình đào tạo)
CREATE TABLE IF NOT EXISTS curriculum (
    curriculum_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    curriculum_code VARCHAR(20) UNIQUE NOT NULL,
    curriculum_name VARCHAR(100) NOT NULL,
    start_year INTEGER,
    end_year INTEGER,
    status VARCHAR(20),
    major_id UUID NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (major_id) REFERENCES major(major_id) ON DELETE CASCADE
);

-- Bảng: Combo (Nhóm môn học - Bắt buộc/Tự chọn)
CREATE TABLE IF NOT EXISTS combo (
    combo_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    combo_name VARCHAR(100) NOT NULL,
    combo_type VARCHAR(50), -- 'Bắt buộc', 'Tự chọn', 'Chuyên ngành'
    description TEXT,
    total_credits INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- KHỐI 2: MÔN HỌC & CHUẨN ĐẦU RA (SUBJECT & OBE)
-- =====================================================

-- Bảng: Subject (Môn học)
CREATE TABLE IF NOT EXISTS subjects (
    subject_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subject_code VARCHAR(20) UNIQUE NOT NULL,
    subject_name VARCHAR(100) NOT NULL,
    credits INTEGER NOT NULL,
    semester VARCHAR(20),
    description TEXT,
    student_limit INTEGER,
    is_approved BOOLEAN DEFAULT FALSE,
    status BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Bảng: PLOs (Program Learning Outcomes - Chuẩn đầu ra ngành)
CREATE TABLE IF NOT EXISTS plos (
    plo_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plo_code VARCHAR(20) NOT NULL,
    plo_name VARCHAR(100) NOT NULL,
    description TEXT,
    major_id UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (major_id) REFERENCES major(major_id) ON DELETE CASCADE
);

-- Bảng: CLOs (Course Learning Outcomes - Chuẩn đầu ra môn học)
-- QUY TẮC QUAN TRỌNG: CLOs gắn với Subject, KHÔNG phải Syllabus
CREATE TABLE IF NOT EXISTS clos (
    clo_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clo_code VARCHAR(20) NOT NULL,
    clo_name VARCHAR(100) NOT NULL,
    description TEXT,
    bloom_level VARCHAR(50), -- Remember, Understand, Apply, Analyze, Evaluate, Create
    subject_id UUID NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (subject_id) REFERENCES subjects(subject_id) ON DELETE CASCADE
);

-- Bảng: CLO_PLO_Mapping (Mapping giữa CLO và PLO)
-- "Môn học này đóng góp gì cho mục tiêu đầu ra của ngành?"
CREATE TABLE IF NOT EXISTS clo_plo_mapping (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clo_id UUID NOT NULL,
    plo_id UUID NOT NULL,
    contribution_level VARCHAR(20), -- 'Low', 'Medium', 'High'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (clo_id) REFERENCES clos(clo_id) ON DELETE CASCADE,
    FOREIGN KEY (plo_id) REFERENCES plos(plo_id) ON DELETE CASCADE,
    UNIQUE (clo_id, plo_id)
);

-- Bảng: Elective (Môn học tự chọn)
CREATE TABLE IF NOT EXISTS elective (
    elective_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    elective_name VARCHAR(100) NOT NULL,
    description TEXT,
    min_credits_required INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- KHỐI 3: ĐỀ CƯƠNG & GIẢNG DẠY (SYLLABUS EXECUTION)
-- =====================================================

-- Bảng: Syllabus (Đề cương chi tiết môn học)
-- Một Subject có nhiều Syllabus (version khác nhau, giảng viên khác nhau)
CREATE TABLE IF NOT EXISTS syllabus (
    syllabus_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subject_id UUID NOT NULL,
    syllabus_name VARCHAR(100) NOT NULL,
    min_bloom_level INTEGER,
    min_avg_grade DECIMAL(4,2),
    status VARCHAR(20), -- 'Draft', 'Review', 'Approved', 'Active', 'Archived'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    approved_date TIMESTAMP,
    FOREIGN KEY (subject_id) REFERENCES subjects(subject_id) ON DELETE CASCADE
);

-- Bảng: Session (Buổi học trong syllabus)
CREATE TABLE IF NOT EXISTS session (
    session_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    syllabus_id UUID NOT NULL,
    session_number INTEGER NOT NULL,
    session_title VARCHAR(200) NOT NULL,
    learning_objectives TEXT,
    content TEXT,
    teaching_methods TEXT,
    duration INTEGER, -- Tính bằng phút
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (syllabus_id) REFERENCES syllabus(syllabus_id) ON DELETE CASCADE
);

-- Bảng: Assessment_Category (Loại đánh giá - Formative/Summative)
CREATE TABLE IF NOT EXISTS assessment_category (
    category_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_name VARCHAR(50) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Bảng: Assessment_Type (Hình thức đánh giá - Quiz, Midterm, Final, Project...)
CREATE TABLE IF NOT EXISTS assessment_type (
    type_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type_name VARCHAR(50) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Bảng: Assessments (Bài đánh giá)
CREATE TABLE IF NOT EXISTS assessments (
    assessment_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id UUID,
    type_id UUID,
    part INTEGER,
    weight DECIMAL(5,2), -- Trọng số %
    completion_criteria TEXT,
    duration INTEGER, -- Phút
    question_type VARCHAR(50),
    knowledge_skill VARCHAR(50),
    grading_guide TEXT,
    note TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES assessment_category(category_id),
    FOREIGN KEY (type_id) REFERENCES assessment_type(type_id)
);

-- Bảng: Assessment_Syllabus (Liên kết Assessment với Syllabus)
CREATE TABLE IF NOT EXISTS assessment_syllabus (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assessment_id UUID NOT NULL,
    syllabus_id UUID NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (assessment_id) REFERENCES assessments(assessment_id) ON DELETE CASCADE,
    FOREIGN KEY (syllabus_id) REFERENCES syllabus(syllabus_id) ON DELETE CASCADE,
    UNIQUE (assessment_id, syllabus_id)
);

-- Bảng: CLO_Assessment (Mapping Assessment với CLO)
-- "Bài kiểm tra này đo lường CLO nào?"
CREATE TABLE IF NOT EXISTS clo_assessment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clo_id UUID NOT NULL,
    assessment_id UUID NOT NULL,
    percentage DECIMAL(5,2), -- % của assessment đo lường CLO này
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (clo_id) REFERENCES clos(clo_id) ON DELETE CASCADE,
    FOREIGN KEY (assessment_id) REFERENCES assessments(assessment_id) ON DELETE CASCADE,
    UNIQUE (clo_id, assessment_id)
);

-- Bảng: Assessment_Templates (Mẫu đánh giá có sẵn)
CREATE TABLE IF NOT EXISTS assessment_templates (
    template_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_name VARCHAR(100) NOT NULL,
    category_id UUID,
    type_id UUID,
    default_weight DECIMAL(5,2),
    default_duration INTEGER,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES assessment_category(category_id),
    FOREIGN KEY (type_id) REFERENCES assessment_type(type_id)
);

-- =====================================================
-- KHỐI 4: QUY TRÌNH SOẠN THẢO (CONTENT WORKFLOW)
-- =====================================================

-- Bảng: Account (Tài khoản người dùng)
CREATE TABLE IF NOT EXISTS account (
    account_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP
);

-- Bảng: Role (Vai trò - Admin, Lecturer, Reviewer, Student)
CREATE TABLE IF NOT EXISTS role (
    role_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_name VARCHAR(50) UNIQUE NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Bảng: Lecturer_Profile (Hồ sơ giảng viên)
CREATE TABLE IF NOT EXISTS lecturer_profile (
    lecturer_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL,
    department_id UUID,
    title VARCHAR(50), -- 'GS', 'PGS', 'TS', 'ThS'
    specialization TEXT,
    bio TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (account_id) REFERENCES account(account_id) ON DELETE CASCADE,
    FOREIGN KEY (department_id) REFERENCES department(department_id)
);

-- Bảng: Sprint (Sprint/Đợt làm việc - Agile approach)
CREATE TABLE IF NOT EXISTS sprint (
    sprint_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sprint_name VARCHAR(100) NOT NULL,
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL,
    status VARCHAR(20) DEFAULT 'Planning', -- 'Planning', 'Active', 'Completed'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Bảng: Sprint_Member (Thành viên tham gia Sprint)
CREATE TABLE IF NOT EXISTS sprint_member (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sprint_id UUID NOT NULL,
    lecturer_id UUID NOT NULL,
    role_in_sprint VARCHAR(50), -- 'Lead', 'Member', 'Reviewer'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (sprint_id) REFERENCES sprint(sprint_id) ON DELETE CASCADE,
    FOREIGN KEY (lecturer_id) REFERENCES lecturer_profile(lecturer_id) ON DELETE CASCADE,
    UNIQUE (sprint_id, lecturer_id)
);

-- Bảng: Task (Công việc trong Sprint)
CREATE TABLE IF NOT EXISTS task (
    task_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sprint_id UUID NOT NULL,
    task_name VARCHAR(200) NOT NULL,
    description TEXT,
    assigned_to UUID, -- lecturer_id
    status VARCHAR(20) DEFAULT 'To Do', -- 'To Do', 'In Progress', 'Review', 'Done'
    priority VARCHAR(20), -- 'Low', 'Medium', 'High', 'Critical'
    due_date TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (sprint_id) REFERENCES sprint(sprint_id) ON DELETE CASCADE,
    FOREIGN KEY (assigned_to) REFERENCES lecturer_profile(lecturer_id)
);

-- Bảng: Syllabus_Review (Đánh giá đề cương)
CREATE TABLE IF NOT EXISTS syllabus_review (
    review_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    syllabus_id UUID NOT NULL,
    reviewer_id UUID NOT NULL,
    review_status VARCHAR(20), -- 'Pending', 'Approved', 'Rejected', 'Needs Revision'
    overall_rating INTEGER CHECK (overall_rating BETWEEN 1 AND 5),
    review_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (syllabus_id) REFERENCES syllabus(syllabus_id) ON DELETE CASCADE,
    FOREIGN KEY (reviewer_id) REFERENCES lecturer_profile(lecturer_id)
);

-- Bảng: Syllabus_Comments (Comments trong quá trình review)
CREATE TABLE IF NOT EXISTS syllabus_comments (
    comment_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    review_id UUID NOT NULL,
    comment_text TEXT NOT NULL,
    comment_type VARCHAR(20), -- 'General', 'Content', 'Assessment', 'CLO'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (review_id) REFERENCES syllabus_review(review_id) ON DELETE CASCADE
);

-- Bảng: Syllabus_Action_Logs (Lịch sử thay đổi đề cương)
CREATE TABLE IF NOT EXISTS syllabus_action_logs (
    log_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    syllabus_id UUID NOT NULL,
    action_by UUID NOT NULL,
    action_type VARCHAR(50) NOT NULL, -- 'Created', 'Updated', 'Submitted', 'Approved', 'Rejected'
    action_description TEXT,
    action_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (syllabus_id) REFERENCES syllabus(syllabus_id) ON DELETE CASCADE,
    FOREIGN KEY (action_by) REFERENCES account(account_id)
);

-- =====================================================
-- KHỐI 5: TÀI NGUYÊN & AI (RESOURCE & AI INTEGRATION)
-- =====================================================

-- Bảng: Source (Nguồn tài liệu)
CREATE TABLE IF NOT EXISTS source (
    source_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_name VARCHAR(100) NOT NULL,
    source_type VARCHAR(50), -- 'Book', 'Journal', 'Website', 'Video'
    author VARCHAR(200),
    publisher VARCHAR(100),
    publication_year INTEGER,
    isbn VARCHAR(20),
    url TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Bảng: Material (Tài liệu học tập)
CREATE TABLE IF NOT EXISTS material (
    material_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    material_name VARCHAR(200) NOT NULL,
    material_type VARCHAR(50), -- 'PDF', 'PPT', 'Video', 'Doc'
    file_path TEXT,
    file_size BIGINT, -- bytes
    upload_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    uploaded_by UUID,
    description TEXT,
    FOREIGN KEY (uploaded_by) REFERENCES account(account_id)
);

-- Bảng: Syllabus_Source (Liên kết giữa Syllabus và Source)
CREATE TABLE IF NOT EXISTS syllabus_source (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    syllabus_id UUID NOT NULL,
    source_id UUID NOT NULL,
    is_required BOOLEAN DEFAULT FALSE, -- Tài liệu bắt buộc hay tham khảo
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (syllabus_id) REFERENCES syllabus(syllabus_id) ON DELETE CASCADE,
    FOREIGN KEY (source_id) REFERENCES source(source_id) ON DELETE CASCADE,
    UNIQUE (syllabus_id, source_id)
);

-- Bảng: Blocks (Khối văn bản từ tài liệu - để vector hóa)
CREATE TABLE IF NOT EXISTS blocks (
    block_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    material_id UUID NOT NULL,
    block_sequence INTEGER NOT NULL, -- Thứ tự block trong document
    content TEXT NOT NULL,
    page_number INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (material_id) REFERENCES material(material_id) ON DELETE CASCADE
);

-- Bảng: Vector_Embeddings (Vector embedding cho semantic search)
CREATE TABLE IF NOT EXISTS vector_embeddings (
    embedding_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    block_id UUID NOT NULL,
    embedding_vector REAL[], -- Hoặc dùng pgvector extension: vector(1536)
    model_name VARCHAR(50), -- 'text-embedding-ada-002', 'sentence-transformers/all-MiniLM-L6-v2'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (block_id) REFERENCES blocks(block_id) ON DELETE CASCADE
);

-- Bảng: Student_Material_Tracking (Tracking tiến độ đọc tài liệu)
CREATE TABLE IF NOT EXISTS student_material_tracking (
    tracking_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID NOT NULL,
    material_id UUID NOT NULL,
    progress_percentage DECIMAL(5,2) DEFAULT 0.00,
    last_accessed TIMESTAMP,
    completed BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES account(account_id) ON DELETE CASCADE,
    FOREIGN KEY (material_id) REFERENCES material(material_id) ON DELETE CASCADE,
    UNIQUE (student_id, material_id)
);

-- Bảng: Notification (Thông báo cho người dùng)
CREATE TABLE IF NOT EXISTS notification (
    notification_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_id UUID NOT NULL,
    notification_type VARCHAR(50), -- 'Task', 'Review', 'Deadline', 'System'
    title VARCHAR(200) NOT NULL,
    message TEXT,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (recipient_id) REFERENCES account(account_id) ON DELETE CASCADE
);

-- Bảng: System_Log (Hệ thống log)
CREATE TABLE IF NOT EXISTS system_log (
    log_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    log_level VARCHAR(20), -- 'INFO', 'WARN', 'ERROR'
    log_message TEXT,
    log_source VARCHAR(100), -- Tên service/component
    user_id UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES account(account_id)
);

-- =====================================================
-- BẢNG QUAN HỆ TRUNG GIAN (MANY-TO-MANY)
-- =====================================================

-- Bảng: Curriculum_Combo (Curriculum chứa nhiều Combo)
CREATE TABLE IF NOT EXISTS curriculum_combo (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    curriculum_id UUID NOT NULL,
    combo_id UUID NOT NULL,
    semester INTEGER,
    is_required BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (curriculum_id) REFERENCES curriculum(curriculum_id) ON DELETE CASCADE,
    FOREIGN KEY (combo_id) REFERENCES combo(combo_id) ON DELETE CASCADE,
    UNIQUE (curriculum_id, combo_id)
);

-- Bảng: Combo_Subject (Combo chứa nhiều Subject)
CREATE TABLE IF NOT EXISTS combo_subject (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    combo_id UUID NOT NULL,
    subject_id UUID NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (combo_id) REFERENCES combo(combo_id) ON DELETE CASCADE,
    FOREIGN KEY (subject_id) REFERENCES subjects(subject_id) ON DELETE CASCADE,
    UNIQUE (combo_id, subject_id)
);

-- Bảng: Curriculum_Combo_Subject (Alternative: Direct link từ Curriculum đến Subject qua Combo)
CREATE TABLE IF NOT EXISTS curriculum_combo_subject (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    curriculum_id UUID NOT NULL,
    combo_id UUID NOT NULL,
    subject_id UUID NOT NULL,
    semester_recommended INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (curriculum_id) REFERENCES curriculum(curriculum_id) ON DELETE CASCADE,
    FOREIGN KEY (combo_id) REFERENCES combo(combo_id) ON DELETE CASCADE,
    FOREIGN KEY (subject_id) REFERENCES subjects(subject_id) ON DELETE CASCADE,
    UNIQUE (curriculum_id, combo_id, subject_id)
);

-- Bảng: Subject_WishList (Danh sách môn học yêu thích của sinh viên)
CREATE TABLE IF NOT EXISTS subject_wishlist (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID NOT NULL,
    subject_id UUID NOT NULL,
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES account(account_id) ON DELETE CASCADE,
    FOREIGN KEY (subject_id) REFERENCES subjects(subject_id) ON DELETE CASCADE,
    UNIQUE (student_id, subject_id)
);

-- =====================================================
-- INDEXES for Performance Optimization
-- =====================================================

-- Indexes cho Foreign Keys (quan trọng cho JOIN performance)
CREATE INDEX idx_curriculum_major_id ON curriculum(major_id);
CREATE INDEX idx_clos_subject_id ON clos(subject_id);
CREATE INDEX idx_plos_major_id ON plos(major_id);
CREATE INDEX idx_syllabus_subject_id ON syllabus(subject_id);
CREATE INDEX idx_session_syllabus_id ON session(syllabus_id);
CREATE INDEX idx_lecturer_profile_account_id ON lecturer_profile(account_id);
CREATE INDEX idx_task_sprint_id ON task(sprint_id);
CREATE INDEX idx_task_assigned_to ON task(assigned_to);
CREATE INDEX idx_blocks_material_id ON blocks(material_id);
CREATE INDEX idx_vector_embeddings_block_id ON vector_embeddings(block_id);

-- Indexes cho Lookup thường xuyên
CREATE INDEX idx_subjects_code ON subjects(subject_code);
CREATE INDEX idx_subjects_name ON subjects(subject_name);
CREATE INDEX idx_major_code ON major(major_code);
CREATE INDEX idx_account_username ON account(username);
CREATE INDEX idx_account_email ON account(email);
CREATE INDEX idx_syllabus_status ON syllabus(status);
CREATE INDEX idx_task_status ON task(status);
CREATE INDEX idx_notification_recipient ON notification(recipient_id);
CREATE INDEX idx_notification_is_read ON notification(is_read);

-- Composite Indexes cho query phức tạp
CREATE INDEX idx_clo_plo_mapping_clo_plo ON clo_plo_mapping(clo_id, plo_id);
CREATE INDEX idx_student_tracking_student_material ON student_material_tracking(student_id, material_id);
CREATE INDEX idx_sprint_member_sprint_lecturer ON sprint_member(sprint_id, lecturer_id);

-- =====================================================
-- INITIAL DATA (Seed Data)
-- =====================================================

-- Seed: Assessment Categories
INSERT INTO assessment_category (category_id, category_name, description) VALUES
    (gen_random_uuid(), 'Formative', 'Đánh giá thường xuyên trong quá trình học'),
    (gen_random_uuid(), 'Summative', 'Đánh giá tổng kết cuối kỳ')
ON CONFLICT DO NOTHING;

-- Seed: Assessment Types
INSERT INTO assessment_type (type_id, type_name, description) VALUES
    (gen_random_uuid(), 'Quiz', 'Kiểm tra nhanh'),
    (gen_random_uuid(), 'Midterm', 'Kiểm tra giữa kỳ'),
    (gen_random_uuid(), 'Final', 'Thi cuối kỳ'),
    (gen_random_uuid(), 'Project', 'Đồ án, dự án'),
    (gen_random_uuid(), 'Presentation', 'Thuyết trình'),
    (gen_random_uuid(), 'Lab', 'Thực hành')
ON CONFLICT DO NOTHING;

-- Seed: Roles
INSERT INTO role (role_id, role_name, description) VALUES
    (gen_random_uuid(), 'ADMIN', 'Quản trị viên hệ thống'),
    (gen_random_uuid(), 'LECTURER', 'Giảng viên'),
    (gen_random_uuid(), 'REVIEWER', 'Người đánh giá đề cương'),
    (gen_random_uuid(), 'STUDENT', 'Sinh viên'),
    (gen_random_uuid(), 'HEAD_OF_DEPARTMENT', 'Trưởng bộ môn')
ON CONFLICT DO NOTHING;

-- =====================================================
-- COMMENTS for Documentation
-- =====================================================

COMMENT ON TABLE subjects IS 'Môn học - Định nghĩa chuẩn của môn, không phụ thuộc vào giảng viên';
COMMENT ON TABLE clos IS 'Course Learning Outcomes - Chuẩn đầu ra MÔN HỌC (gắn với Subject)';
COMMENT ON TABLE plos IS 'Program Learning Outcomes - Chuẩn đầu ra NGÀNH HỌC (gắn với Major)';
COMMENT ON TABLE syllabus IS 'Đề cương chi tiết - Triển khai cụ thể của môn học (có thể nhiều version)';
COMMENT ON TABLE clo_plo_mapping IS 'Mapping CLO-PLO: Môn học đóng góp gì cho chuẩn đầu ra ngành';
COMMENT ON TABLE clo_assessment IS 'Mapping CLO-Assessment: Bài kiểm tra đo lường CLO nào';
COMMENT ON TABLE sprint IS 'Sprint Agile - Đợt làm việc soạn thảo đề cương';
COMMENT ON TABLE vector_embeddings IS 'Vector embeddings cho RAG/Semantic Search với AI';

-- =====================================================
-- END OF MIGRATION
-- =====================================================
