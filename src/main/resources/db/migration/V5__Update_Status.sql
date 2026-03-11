-- =====================================================
-- Migration: Update Relations and Table Structure
-- Version: V5
-- =====================================================

-- 1. Thay đổi kiểu dữ liệu cột status của bảng subjects
-- Sử dụng USING để convert dữ liệu cũ: true -> 'ACTIVE', false -> 'INACTIVE'
ALTER TABLE subjects
ALTER COLUMN status TYPE VARCHAR(20)
    USING (CASE
        WHEN status IS TRUE THEN 'ACTIVE'
        WHEN status IS FALSE THEN 'INACTIVE'
        ELSE 'DRAFT'
    END);

-- Thiết lập giá trị mặc định mới là chuỗi
ALTER TABLE subjects ALTER COLUMN status SET DEFAULT 'DRAFT';

-- 2. Thêm cột curriculum_id vào bảng plos
ALTER TABLE plos
    ADD COLUMN IF NOT EXISTS curriculum_id UUID;

-- 3. Thêm ràng buộc khóa ngoại (Foreign Key)
-- Lưu ý: Kiểm tra xem bảng 'curriculums' đã tồn tại và cột 'curriculum_id' đúng tên chưa
ALTER TABLE plos
    ADD CONSTRAINT fk_plos_curriculum
        FOREIGN KEY (curriculum_id)
            REFERENCES curriculum(curriculum_id)
            ON DELETE SET NULL;

-- 4. Tạo Index để tối ưu truy vấn PLO theo Curriculum (Dành cho Bridge Engineer)
CREATE INDEX IF NOT EXISTS idx_plos_curriculum_id ON plos(curriculum_id);
