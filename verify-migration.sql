-- Verify Migration Script
-- Kiểm tra tất cả các bảng đã được tạo

-- 1. Đếm số bảng
SELECT COUNT(*) as total_tables
FROM pg_tables
WHERE schemaname = 'public';

-- 2. Liệt kê tất cả các bảng (theo khối chức năng)
SELECT
    '=== KHỐI 1: KHUNG CHƯƠNG TRÌNH ===' as section,
    string_agg(tablename, ', ' ORDER BY tablename) as tables
FROM pg_tables
WHERE schemaname = 'public'
AND tablename IN ('major', 'curriculum', 'combo', 'department')

UNION ALL

SELECT
    '=== KHỐI 2: MÔN HỌC & OBE ===' as section,
    string_agg(tablename, ', ' ORDER BY tablename) as tables
FROM pg_tables
WHERE schemaname = 'public'
AND tablename IN ('subjects', 'clos', 'plos', 'clo_plo_mapping', 'elective')

UNION ALL

SELECT
    '=== KHỐI 3: ĐỀ CƯƠNG ===' as section,
    string_agg(tablename, ', ' ORDER BY tablename) as tables
FROM pg_tables
WHERE schemaname = 'public'
AND tablename IN ('syllabus', 'session', 'assessments', 'assessment_category',
                  'assessment_type', 'assessment_syllabus', 'clo_assessment',
                  'assessment_templates')

UNION ALL

SELECT
    '=== KHỐI 4: QUY TRÌNH ===' as section,
    string_agg(tablename, ', ' ORDER BY tablename) as tables
FROM pg_tables
WHERE schemaname = 'public'
AND tablename IN ('account', 'role', 'lecturer_profile', 'sprint', 'task',
                  'sprint_member', 'syllabus_review', 'syllabus_comments',
                  'syllabus_action_logs')

UNION ALL

SELECT
    '=== KHỐI 5: TÀI NGUYÊN & AI ===' as section,
    string_agg(tablename, ', ' ORDER BY tablename) as tables
FROM pg_tables
WHERE schemaname = 'public'
AND tablename IN ('material', 'source', 'blocks', 'vector_embeddings',
                  'student_material_tracking', 'notification', 'system_log',
                  'syllabus_source');

-- 3. Kiểm tra seed data
SELECT 'Assessment Categories' as data_type, COUNT(*) as count FROM assessment_category
UNION ALL
SELECT 'Assessment Types' as data_type, COUNT(*) as count FROM assessment_type
UNION ALL
SELECT 'Roles' as data_type, COUNT(*) as count FROM role;

-- 4. Kiểm tra flyway history
SELECT installed_rank, version, description, type, installed_on, success
FROM flyway_schema_history
ORDER BY installed_rank;
