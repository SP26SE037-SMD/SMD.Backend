-- =====================================================
-- Migration: Update Relations and Table Structure
-- Version: V4
-- Created: 2026-03-08
-- =====================================================
-- Description:
-- Update Combo and Curriculum_Combo_Subject tables
-- to match with entity definitions
-- =====================================================

-- =====================================================
-- 1. CẬP NHẬT BẢNG COMBO
-- =====================================================

-- Thêm cột combo_code (bắt buộc và unique theo entity)
ALTER TABLE combo
    ADD COLUMN IF NOT EXISTS combo_code VARCHAR(20);

-- Cập nhật combo_code cho các bản ghi hiện có (nếu có)
-- Nếu chưa có dữ liệu thì bỏ qua, nếu có dữ liệu cần tạo code tự động
UPDATE combo
SET combo_code = CONCAT('COMBO-', LEFT(combo_id::TEXT, 8))
WHERE combo_code IS NULL;


-- Xóa cột total_credits (không có trong entity)
ALTER TABLE combo
    DROP COLUMN IF EXISTS total_credits;


-- =====================================================
-- 2. CẬP NHẬT BẢNG CURRICULUM_COMBO_SUBJECT
-- =====================================================

-- Đổi tên cột semester_recommended thành semester để khớp với entity
--ALTER TABLE curriculum_combo_subject
--    RENAME COLUMN semester_recommended TO semester;

-- Xóa cột created_at (không có trong entity)
ALTER TABLE curriculum_combo_subject
    DROP COLUMN IF EXISTS created_at;

-- =====================================================
-- 3. THÊM INDEXES ĐỂ TỐI ƯU HIỆU NĂNG
-- =====================================================

-- Indexes cho bảng curriculum_combo_subject
CREATE INDEX IF NOT EXISTS idx_curriculum_combo_subject_curriculum_id
    ON curriculum_combo_subject(curriculum_id);

CREATE INDEX IF NOT EXISTS idx_curriculum_combo_subject_combo_id
    ON curriculum_combo_subject(combo_id);

CREATE INDEX IF NOT EXISTS idx_curriculum_combo_subject_subject_id
    ON curriculum_combo_subject(subject_id);

CREATE INDEX IF NOT EXISTS idx_curriculum_combo_subject_semester
    ON curriculum_combo_subject(semester);

-- Indexes cho bảng combo
CREATE INDEX IF NOT EXISTS idx_combo_type
    ON combo(type);

-- =====================================================
-- 4. CẬP NHẬT UNIQUE CONSTRAINT
-- =====================================================

-- Đảm bảo unique constraint chính xác cho curriculum_combo_subject
-- Drop constraint cũ nếu có
ALTER TABLE curriculum_combo_subject
    DROP CONSTRAINT IF EXISTS curriculum_combo_subject_curriculum_id_combo_id_subject_id_key;

-- Thêm constraint mới với tên rõ ràng
--ALTER TABLE curriculum_combo_subject
--    ADD CONSTRAINT uk_curriculum_combo_subject_unique
--    UNIQUE (curriculum_id, combo_id, subject_id);

Drop Table IF EXISTS combo_subject;
Drop Table IF EXISTS curriculum_combo;

-- =====================================================
-- 5. THÊM COMMENTS CHO CÁC BẢNG VÀ CỘT
-- =====================================================

COMMENT ON TABLE combo IS 'Bảng quản lý các nhóm môn học (bắt buộc/tự chọn)';
COMMENT ON COLUMN combo.combo_code IS 'Mã nhóm môn học (unique)';
COMMENT ON COLUMN combo.combo_name IS 'Tên nhóm môn học';
COMMENT ON COLUMN combo.type IS 'Loại nhóm: Elective (Tự chọn) / Mandatory (Bắt buộc)';

COMMENT ON TABLE curriculum_combo_subject IS 'Bảng quan hệ giữa Curriculum, Combo và Subject - quản lý môn học trong từng học kỳ của khung chương trình';
COMMENT ON COLUMN curriculum_combo_subject.semester IS 'Học kỳ khuyến nghị học môn này';
COMMENT ON COLUMN curriculum_combo_subject.curriculum_id IS 'ID khung chương trình';
COMMENT ON COLUMN curriculum_combo_subject.combo_id IS 'ID nhóm môn học';
COMMENT ON COLUMN curriculum_combo_subject.subject_id IS 'ID môn học';


