ALTER TABLE public.curriculum DROP CONSTRAINT curriculum_major_id_fkey;
ALTER TABLE public.curriculum DROP COLUMN major_id;


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

