ALTER TABLE public.task_v2 ADD is_accepted bool NULL;
ALTER TABLE public.review_v2 DROP COLUMN is_accepted;
