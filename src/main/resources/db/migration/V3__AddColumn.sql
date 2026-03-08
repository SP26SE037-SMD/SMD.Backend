-- =====================================================
-- Migration: Additional Entity Updates
-- Version: V3
-- =====================================================

-- 1. Xóa cột username khỏi bảng account
ALTER TABLE account
    DROP COLUMN IF EXISTS username;

-- 2. Cập nhật bảng CLOs
ALTER TABLE clos
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN',
    ADD COLUMN IF NOT EXISTS created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

 -- 3. Cập nhật bảng Materials
ALTER TABLE material
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN';

-- 4. Cập nhật bảng Session
ALTER TABLE session
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN';

-- 5. Cập nhật bảng Assessment
ALTER TABLE assessments
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN',
    ADD COLUMN IF NOT EXISTS created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
