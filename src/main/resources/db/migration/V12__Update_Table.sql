ALTER TABLE public.task ALTER COLUMN sprint_id DROP NOT NULL;
ALTER TABLE public.task ALTER COLUMN assigned_to SET NOT NULL;
ALTER TABLE public.task ADD curriculum_id uuid NULL;
ALTER TABLE public.task ADD CONSTRAINT task_curriculum_fk FOREIGN KEY (curriculum_id) REFERENCES public.curriculum(curriculum_id);
