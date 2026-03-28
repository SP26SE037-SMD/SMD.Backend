ALTER TABLE public.sprint ADD curriculum_id uuid NULL;
ALTER TABLE public.sprint ADD CONSTRAINT sprint_curriculum_fk FOREIGN KEY (curriculum_id) REFERENCES public.curriculum(curriculum_id);


ALTER TABLE public.task DROP CONSTRAINT task_curriculum_fk;
ALTER TABLE public.task DROP COLUMN curriculum_id;
ALTER TABLE public.task ADD subject_id uuid NULL;
ALTER TABLE public.task ADD CONSTRAINT task_subjects_fk FOREIGN KEY (subject_id) REFERENCES public.subjects(subject_id);
