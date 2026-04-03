ALTER TABLE public.notification
    ADD COLUMN IF NOT EXISTS task_id uuid NULL;

ALTER TABLE public.notification
    ADD COLUMN IF NOT EXISTS review_id uuid NULL;
