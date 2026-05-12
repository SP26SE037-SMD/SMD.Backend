ALTER TABLE public.request ADD target_id uuid NULL;
ALTER TABLE public.request ADD request_type varchar(50) NULL;
ALTER TABLE public.request DROP CONSTRAINT request_curriculum_fk;
ALTER TABLE public.request DROP COLUMN curriculum_id;
ALTER TABLE public.request DROP CONSTRAINT request_major_fk;
ALTER TABLE public.request DROP COLUMN major_id;
ALTER TABLE public.request DROP CONSTRAINT request_task_fk;
ALTER TABLE public.request DROP COLUMN task_id;
