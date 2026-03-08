-- =====================================================
-- Migration: Update Subjects Structure and System Log
-- Version: V2
-- =====================================================

-- 1. Xóa cột semester
ALTER TABLE subjects DROP COLUMN IF EXISTS semester;

-- 2. Bổ sung các cột thiếu (Lưu ý: bỏ dấu phẩy ở dòng cuối và thêm dấu chấm phẩy)
ALTER TABLE subjects
    ADD COLUMN IF NOT EXISTS degree_level VARCHAR(20),
    ADD COLUMN IF NOT EXISTS time_allocation VARCHAR(50),
    ADD COLUMN IF NOT EXISTS student_tasks VARCHAR(100),
    ADD COLUMN IF NOT EXISTS scoring_scale INTEGER,
    ADD COLUMN IF NOT EXISTS decision_no VARCHAR(50),
    ADD COLUMN IF NOT EXISTS approved_date TIMESTAMP,
    ADD COLUMN IF NOT EXISTS min_to_pass INTEGER; -- Đã bỏ dấu phẩy, thêm dấu chấm phẩy

-- 3. Cập nhật kiểu dữ liệu cho description
ALTER TABLE subjects ALTER COLUMN description TYPE TEXT;

ALTER TABLE elective
    ADD COLUMN IF NOT EXISTS elective_code VARCHAR(20) NOT NULL UNIQUE;

-- 4. Cập nhật bảng system_log
ALTER TABLE system_log
DROP COLUMN IF EXISTS log_source,
DROP COLUMN IF EXISTS log_level,
ADD COLUMN IF NOT EXISTS action VARCHAR(100) NOT NULL DEFAULT 'UNKNOWN',
ADD COLUMN IF NOT EXISTS target_id UUID;

-- 5. Đổi tên bảng lecturer_profile thành account_profile
ALTER TABLE IF EXISTS lecturer_profile RENAME TO account_profile;

-- 6. Đổi tên cột lecturer_id thành profile_id và chuyển thành UUID
ALTER TABLE IF EXISTS account_profile
    DROP COLUMN IF EXISTS department_id,
    ALTER COLUMN lecturer_id TYPE UUID USING lecturer_id::UUID;

ALTER TABLE IF EXISTS account_profile
    RENAME COLUMN lecturer_id TO profile_id;
