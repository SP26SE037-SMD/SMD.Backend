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

-- 1. Xóa các cột không còn sử dụng
ALTER TABLE blocks DROP COLUMN IF EXISTS block_sequence;
ALTER TABLE blocks DROP COLUMN IF EXISTS page_number;

-- 2. Thêm cột idx để quản lý thứ tự (Kiểu Integer)
ALTER TABLE blocks ADD COLUMN idx INT4 DEFAULT 0;

-- 3. Thêm ràng buộc NOT NULL cho idx nếu cần thiết
ALTER TABLE blocks ALTER COLUMN idx SET NOT NULL;

ALTER TABLE blocks ADD COLUMN IF NOT EXISTS block_style TEXT;

-- 1. Đổi tên cột từ model_name sang content
ALTER TABLE vector_embeddings
    RENAME COLUMN model_name TO content;

-- 2. Thay đổi kiểu dữ liệu sang TEXT
ALTER TABLE vector_embeddings
ALTER COLUMN content TYPE TEXT;

-- 0. Kích hoạt extension pgvector (BẮT BUỘC)
CREATE EXTENSION IF NOT EXISTS vector;

-- 1. Xóa cột cũ (nếu có) để tránh xung đột kiểu dữ liệu
ALTER TABLE vector_embeddings DROP COLUMN IF EXISTS embedding_vector;

-- 2. Thêm cột mới với kiểu vector chuyên dụng
-- Lưu ý: Gemini model 'text-embedding-004' trả về 768 chiều.
-- Nếu bạn chắc chắn dùng bản 'text-embedding-004', hãy để vector(768) để tối ưu bộ nhớ.
ALTER TABLE vector_embeddings ADD COLUMN embedding_vector vector(3072);
