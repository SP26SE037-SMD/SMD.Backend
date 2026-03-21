-- Rename mapping table to align with Group terminology
ALTER TABLE IF EXISTS public.curriculum_combo_subject
    RENAME TO curriculum_group_subject;

-- Ensure group_id exists on the renamed mapping table
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'curriculum_group_subject'
          AND column_name = 'combo_id'
    ) THEN
        EXECUTE 'ALTER TABLE public.curriculum_group_subject RENAME COLUMN combo_id TO group_id';
    END IF;
END $$;

-- Rename permission codes from COMBO_* to GROUP_* to match controller/security updates
UPDATE permission
SET permission_name = 'GROUP_CREATE', description = 'Create new group'
WHERE permission_name = 'COMBO_CREATE';

UPDATE permission
SET permission_name = 'GROUP_UPDATE', description = 'Update group information'
WHERE permission_name = 'COMBO_UPDATE';

UPDATE permission
SET permission_name = 'GROUP_READ', description = 'View group information'
WHERE permission_name = 'COMBO_READ';
