ALTER TABLE public.curriculum DROP CONSTRAINT curriculum_major_id_fkey;
ALTER TABLE public.curriculum DROP COLUMN major_id;

ALTER TABLE public.curriculum_combo_subject ALTER COLUMN combo_id DROP NOT NULL;


-- Tạo bảng po nếu chưa tồn tại
CREATE TABLE IF NOT EXISTS po (
    po_id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    po_code    VARCHAR(20) NOT NULL,
    po_name    VARCHAR(200) NOT NULL,
    description TEXT,
    status     VARCHAR(20),
    major_id   UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (major_id) REFERENCES public.major(major_id) ON DELETE SET NULL
);

-- Tạo bảng po_plo_mapping nếu chưa tồn tại
CREATE TABLE IF NOT EXISTS po_plo_mapping (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    po_id      UUID NOT NULL,
    plo_id     UUID NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (po_id, plo_id),
    FOREIGN KEY (po_id)  REFERENCES po(po_id)   ON DELETE CASCADE,
    FOREIGN KEY (plo_id) REFERENCES plos(plo_id) ON DELETE CASCADE
);

-- Xóa cột profile_id (nếu có) trong sprint_member trước khi xóa bảng account_profile
ALTER TABLE public.sprint_member DROP CONSTRAINT IF EXISTS sprint_member_profile_fkey;
ALTER TABLE public.sprint_member DROP CONSTRAINT IF EXISTS sprint_member_profile_id_fkey;
ALTER TABLE public.sprint_member DROP COLUMN IF EXISTS profile_id;

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


--BỔ SUNG THÊM CHỈNH SỬA Mối quan hệ--
ALTER TABLE public.sprint_member RENAME COLUMN lecturer_id TO account_id;
ALTER TABLE public.sprint_member ADD CONSTRAINT sprint_member_account_fk FOREIGN KEY (account_id) REFERENCES public.account(account_id);
ALTER TABLE public.sprint_member DROP CONSTRAINT sprint_member_lecturer_id_fkey;

ALTER TABLE public.task ADD CONSTRAINT tasks_assigned_to_fkey FOREIGN KEY (assigned_to) REFERENCES public.account(account_id);
ALTER TABLE public.task DROP CONSTRAINT task_assigned_to_fkey;
ALTER TABLE public.task RENAME CONSTRAINT tasks_assigned_to_fkey TO task_assigned_to_fkey;

DROP TABLE public.syllabus_comments;
DROP TABLE public.syllabus_review;
DROP TABLE public.account_profile;


ALTER TABLE public.account ADD avatar_url text NULL;
ALTER TABLE public.account ADD phone_number varchar(20) NULL;


ALTER TABLE public.plos DROP CONSTRAINT plos_major_id_fkey;

DROP INDEX public.idx_plos_major_id;
ALTER TABLE public.plos DROP COLUMN major_id;


ALTER TABLE public.curriculum ADD major_id uuid NOT NULL;
ALTER TABLE public.curriculum ADD CONSTRAINT curriculum_major_fk FOREIGN KEY (major_id) REFERENCES public.major(major_id);





-- =====================================================
-- Thêm COMBO Permissions
-- =====================================================
INSERT INTO permission (permission_id, permission_name, description) VALUES
    (gen_random_uuid(), 'COMBO_CREATE', 'Create new combo'),
    (gen_random_uuid(), 'COMBO_UPDATE', 'Update combo information'),
    (gen_random_uuid(), 'COMBO_READ', 'View combo information')
ON CONFLICT (permission_name) DO NOTHING;

-- =====================================================
-- Thêm ACCOUNT Permissions (nếu chưa có)
-- =====================================================
INSERT INTO permission (permission_id, permission_name, description) VALUES
    (gen_random_uuid(), 'ACCOUNT_CREATE', 'Create new account'),
    (gen_random_uuid(), 'ACCOUNT_VIEW_ALL', 'View all accounts with pagination and search'),
    (gen_random_uuid(), 'ACCOUNT_VIEW_DETAIL', 'View account detail by ID'),
    (gen_random_uuid(), 'ACCOUNT_UPDATE', 'Update account information'),
    (gen_random_uuid(), 'ACCOUNT_DELETE', 'Delete account (soft delete)')
ON CONFLICT (permission_name) DO NOTHING;

-- =====================================================
-- Thêm CURRICULUM_DELETE permission (nếu chưa có)
-- =====================================================
INSERT INTO permission (permission_id, permission_name, description) VALUES
    (gen_random_uuid(), 'CURRICULUM_DELETE', 'Delete curriculum')
ON CONFLICT (permission_name) DO NOTHING;

-- =====================================================
-- Thêm ACCOUNT_PROFILE Permissions
-- =====================================================
INSERT INTO permission (permission_id, permission_name, description) VALUES
    (gen_random_uuid(), 'ACCOUNT_PROFILE_VIEW', 'View account profile'),
    (gen_random_uuid(), 'ACCOUNT_PROFILE_UPDATE', 'Update account profile')
ON CONFLICT (permission_name) DO NOTHING;

-- =====================================================
-- Thêm ROLE CRU Permissions (nếu chưa có từ V1)
-- =====================================================
INSERT INTO permission (permission_id, permission_name, description) VALUES
    (gen_random_uuid(), 'ROLE_CREATE', 'Create new role'),
    (gen_random_uuid(), 'ROLE_UPDATE', 'Update role information'),
    (gen_random_uuid(), 'ROLE_DELETE', 'Delete role'),
    (gen_random_uuid(), 'ROLE_VIEW', 'View role information')
ON CONFLICT (permission_name) DO NOTHING;

-- =====================================================
-- Gán COMBO Permissions cho ADMIN
-- =====================================================
INSERT INTO role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM role r
CROSS JOIN permission p
WHERE r.role_name = 'ADMIN'
AND p.permission_name IN (
    'COMBO_CREATE', 'COMBO_UPDATE', 'COMBO_READ',
    'ACCOUNT_CREATE', 'ACCOUNT_VIEW_ALL', 'ACCOUNT_VIEW_DETAIL', 'ACCOUNT_UPDATE', 'ACCOUNT_DELETE',
    'ACCOUNT_PROFILE_VIEW', 'ACCOUNT_PROFILE_UPDATE',
    'ROLE_CREATE', 'ROLE_UPDATE', 'ROLE_DELETE', 'ROLE_VIEW',
    'CURRICULUM_DELETE'
)
ON CONFLICT DO NOTHING;




