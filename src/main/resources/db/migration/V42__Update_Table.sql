ALTER TABLE public.task_v2 ADD "comment" text NULL;

ALTER TABLE public.review_v2 ADD reviewer_id uuid NULL;
ALTER TABLE public.review_v2 ADD CONSTRAINT review_v2_account_fk FOREIGN KEY (reviewer_id) REFERENCES public.account(account_id);
