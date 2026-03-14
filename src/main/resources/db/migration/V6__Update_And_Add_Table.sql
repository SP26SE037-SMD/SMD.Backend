ALTER TABLE public.curriculum_combo_subject ALTER COLUMN combo_id DROP NOT NULL;

-- Bảng lưu JWT đã bị logout để chặn dùng lại trước khi hết hạn
CREATE TABLE IF NOT EXISTS invalidated_token (
    id VARCHAR(255) PRIMARY KEY,
    expiry_date TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_invalidated_token_expiry_date
    ON invalidated_token(expiry_date);


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
-- Thêm POs Permissions
-- =====================================================
INSERT INTO permission (permission_id, permission_name, description) VALUES
    (gen_random_uuid(), 'POS_CREATE', 'Create new po'),
    (gen_random_uuid(), 'POS_UPDATE', 'Update po information'),
    (gen_random_uuid(), 'POS_UPDATE_STATUS', 'Update POs status'),
    (gen_random_uuid(), 'POS_DELETE', 'Delete soft po')
ON CONFLICT (permission_name) DO NOTHING;
