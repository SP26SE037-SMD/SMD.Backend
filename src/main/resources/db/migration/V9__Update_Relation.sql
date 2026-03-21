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

-- 0. Kích hoạt extension pgvector (BẮT BUỘC)
CREATE EXTENSION IF NOT EXISTS vector;

-- 1. Xóa cột cũ (nếu có) để tránh xung đột kiểu dữ liệu
ALTER TABLE vector_embeddings DROP COLUMN IF EXISTS embedding_vector;

-- 2. Thêm cột mới với kiểu vector chuyên dụng
-- Lưu ý: Gemini model 'text-embedding-004' trả về 768 chiều.
-- Nếu bạn chắc chắn dùng bản 'text-embedding-004', hãy để vector(768) để tối ưu bộ nhớ.
ALTER TABLE vector_embeddings ADD COLUMN embedding_vector vector(3072);
