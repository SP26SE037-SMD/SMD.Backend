ALTER TABLE public.combo RENAME TO "group";
ALTER TABLE "group" RENAME COLUMN combo_id TO group_id;
ALTER TABLE "group" RENAME COLUMN combo_name TO group_name;
ALTER TABLE "group" RENAME COLUMN combo_type TO group_type;
ALTER TABLE public."group" RENAME COLUMN combo_code TO group_code;


 ALTER TABLE public.curriculum_combo_subject RENAME COLUMN combo_id TO group_id;
