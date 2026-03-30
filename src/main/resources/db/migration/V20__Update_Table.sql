ALTER TABLE public.task ALTER COLUMN assigned_to DROP NOT NULL;

ALTER TABLE public.review_task ADD due_to timestamp NULL;
