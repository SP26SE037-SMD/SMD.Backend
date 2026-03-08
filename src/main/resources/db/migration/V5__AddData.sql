-- 6. THÊM PERMISSIONS CHO CÁC MODULE
-- =====================================================
-- Insert các permissions còn thiếu từ controllers
-- Note: V1 đã có: ACCOUNT_*, ROLE_*, PERMISSION_*, SYLLABUS_*

INSERT INTO permission (permission_id, permission_name, description) VALUES
    -- Curriculum Management
    (gen_random_uuid(), 'CURRICULUM_CREATE', 'Create new curriculum'),
    (gen_random_uuid(), 'CURRICULUM_UPDATE', 'Update curriculum information'),
    (gen_random_uuid(), 'CURRICULUM_READ', 'View curriculum information'),

    -- Major Management
    (gen_random_uuid(), 'MAJOR_CREATE', 'Create new major'),
    (gen_random_uuid(), 'MAJOR_UPDATE', 'Update major information'),
    (gen_random_uuid(), 'MAJOR_DELETE', 'Delete major'),
    (gen_random_uuid(), 'MAJOR_READ', 'View major information'),

    -- Department Management
    (gen_random_uuid(), 'DEPARTMENT_CREATE', 'Create new department'),
    (gen_random_uuid(), 'DEPARTMENT_UPDATE', 'Update department information'),
    (gen_random_uuid(), 'DEPARTMENT_DELETE', 'Delete department'),
    (gen_random_uuid(), 'DEPARTMENT_READ', 'View department information'),

    -- Subject Management
    (gen_random_uuid(), 'SUBJECT_CREATE', 'Create new subject'),
    (gen_random_uuid(), 'SUBJECT_UPDATE', 'Update subject information'),
    (gen_random_uuid(), 'SUBJECT_DELETE', 'Delete subject'),
    (gen_random_uuid(), 'SUBJECT_READ', 'View subject information'),
    (gen_random_uuid(), 'SUBJECT_PUBLISH', 'Publish/approve subject'),

    -- CLOs Management
    (gen_random_uuid(), 'CLOS_CREATE', 'Create new CLO'),
    (gen_random_uuid(), 'CLOS_UPDATE', 'Update CLO information'),
    (gen_random_uuid(), 'CLOS_DELETE', 'Delete CLO'),
    (gen_random_uuid(), 'CLOS_READ', 'View CLO information'),
    (gen_random_uuid(), 'CLOS_GENERATE', 'Generate CLOs using AI'),
    (gen_random_uuid(), 'CLOS_CHECK', 'Check CLOs quality/validity'),

    -- PLOs Management
    (gen_random_uuid(), 'PLOS_CREATE', 'Create new PLO'),
    (gen_random_uuid(), 'PLOS_UPDATE', 'Update PLO information'),
    (gen_random_uuid(), 'PLOS_DELETE', 'Delete PLO'),
    (gen_random_uuid(), 'PLOS_READ', 'View PLO information'),

    -- Elective Management
    (gen_random_uuid(), 'ELECTIVE_CREATE', 'Create new elective group'),
    (gen_random_uuid(), 'ELECTIVE_UPDATE', 'Update elective group'),
    (gen_random_uuid(), 'ELECTIVE_DELETE', 'Delete elective group'),
    (gen_random_uuid(), 'ELECTIVE_READ', 'View elective information'),
    (gen_random_uuid(), 'ELECTIVE_MANAGE_SUBJECTS', 'Manage subjects in elective group'),

    -- Prerequisite Management
    (gen_random_uuid(), 'PREREQUISITE_MANAGE_SUBJECTS', 'Manage subject prerequisites'),
    (gen_random_uuid(), 'PREREQUISITE_READ', 'View prerequisites'),

    -- System Log
    (gen_random_uuid(), 'SYSTEM_LOG_VIEW_ALL', 'View all system logs'),
    (gen_random_uuid(), 'SYSTEM_LOG_CREATE', 'Create system log entries'),
    (gen_random_uuid(), 'SYSTEM_LOG_DELETE', 'Delete system logs'),

    -- Notification
    (gen_random_uuid(), 'NOTIFICATION_CREATE', 'Create notifications'),
    (gen_random_uuid(), 'NOTIFICATION_READ', 'View notifications'),
    (gen_random_uuid(), 'NOTIFICATION_UPDATE', 'Update/mark notifications'),
    (gen_random_uuid(), 'NOTIFICATION_DELETE', 'Delete notifications')

ON CONFLICT (permission_name) DO NOTHING;

-- =====================================================
-- 7. GÁN PERMISSIONS CHO ADMIN ROLE
-- =====================================================
-- Gán tất cả các permissions mới cho ADMIN role
INSERT INTO role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM role r
CROSS JOIN permission p
WHERE r.role_name = 'ADMIN'
AND p.permission_name IN (
    'CURRICULUM_CREATE', 'CURRICULUM_UPDATE', 'CURRICULUM_DELETE', 'CURRICULUM_READ',
    'MAJOR_CREATE', 'MAJOR_UPDATE', 'MAJOR_DELETE', 'MAJOR_READ',
    'DEPARTMENT_CREATE', 'DEPARTMENT_UPDATE', 'DEPARTMENT_DELETE', 'DEPARTMENT_READ',
    'SUBJECT_CREATE', 'SUBJECT_UPDATE', 'SUBJECT_DELETE', 'SUBJECT_READ', 'SUBJECT_PUBLISH',
    'CLOS_CREATE', 'CLOS_UPDATE', 'CLOS_DELETE', 'CLOS_READ', 'CLOS_GENERATE', 'CLOS_CHECK',
    'PLOS_CREATE', 'PLOS_UPDATE', 'PLOS_DELETE', 'PLOS_READ',
    'ELECTIVE_CREATE', 'ELECTIVE_UPDATE', 'ELECTIVE_DELETE', 'ELECTIVE_READ', 'ELECTIVE_MANAGE_SUBJECTS',
    'PREREQUISITE_MANAGE_SUBJECTS', 'PREREQUISITE_READ',
    'SYSTEM_LOG_VIEW_ALL', 'SYSTEM_LOG_CREATE', 'SYSTEM_LOG_DELETE',
    'NOTIFICATION_CREATE', 'NOTIFICATION_READ', 'NOTIFICATION_UPDATE', 'NOTIFICATION_DELETE'
)
ON CONFLICT DO NOTHING;

-- =====================================================
-- 8. GÁN MỘT SỐ PERMISSIONS CHO COLLABORATOR ROLE
-- =====================================================
-- COLLABORATOR có thể tạo và cập nhật nội dung, nhưng không xóa
INSERT INTO role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM role r
CROSS JOIN permission p
WHERE r.role_name = 'COLLABORATOR'
AND p.permission_name IN (
    'CURRICULUM_READ',
    'MAJOR_READ',
    'DEPARTMENT_READ',
    'SUBJECT_CREATE', 'SUBJECT_UPDATE', 'SUBJECT_READ',
    'CLOS_CREATE', 'CLOS_UPDATE', 'CLOS_READ', 'CLOS_GENERATE', 'CLOS_CHECK',
    'PLOS_READ',
    'ELECTIVE_READ',
    'PREREQUISITE_READ',
    'NOTIFICATION_READ', 'NOTIFICATION_UPDATE'
)
ON CONFLICT DO NOTHING;

-- =====================================================
-- 9. GÁN PERMISSIONS CHO PDCM ROLE
-- =====================================================
-- PDCM (Program Development Committee) có quyền quản lý chương trình đào tạo
INSERT INTO role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM role r
CROSS JOIN permission p
WHERE r.role_name = 'PDCM'
AND p.permission_name IN (
    'CURRICULUM_CREATE', 'CURRICULUM_UPDATE', 'CURRICULUM_READ', 'CURRICULUM_DELETE',
    'MAJOR_CREATE', 'MAJOR_UPDATE', 'MAJOR_READ',
    'DEPARTMENT_READ',
    'SUBJECT_CREATE', 'SUBJECT_UPDATE', 'SUBJECT_READ', 'SUBJECT_PUBLISH',
    'CLOS_CREATE', 'CLOS_UPDATE', 'CLOS_READ', 'CLOS_GENERATE', 'CLOS_CHECK',
    'PLOS_CREATE', 'PLOS_UPDATE', 'PLOS_READ',
    'ELECTIVE_CREATE', 'ELECTIVE_UPDATE', 'ELECTIVE_READ', 'ELECTIVE_MANAGE_SUBJECTS',
    'PREREQUISITE_MANAGE_SUBJECTS', 'PREREQUISITE_READ',
    'SYLLABUS_REVIEW', 'SYLLABUS_APPROVE',
    'NOTIFICATION_READ'
)
ON CONFLICT DO NOTHING;