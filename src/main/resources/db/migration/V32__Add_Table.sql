ALTER TABLE public.request DROP CONSTRAINT request_account_fk;
ALTER TABLE public.request DROP COLUMN requester_id;
ALTER TABLE public.request DROP CONSTRAINT review_task_account_fk;
ALTER TABLE public.request DROP COLUMN reviewer_id;
ALTER TABLE public.request DROP CONSTRAINT review_task_task_fk;
ALTER TABLE public.request DROP COLUMN task_id;
ALTER TABLE public.request RENAME COLUMN review_date TO created_at;
ALTER TABLE public.request ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE public.request ADD created_by uuid NULL;
ALTER TABLE public.request ADD curriculum_id uuid NULL;
ALTER TABLE public.request ADD major_id uuid NULL;
ALTER TABLE public.request ADD "comment" text NULL;
ALTER TABLE public.request ADD updated_at timestamp NULL;
ALTER TABLE public.request DROP CONSTRAINT review_task_pk;
ALTER TABLE public.request ADD CONSTRAINT request_unique UNIQUE (request_id);
ALTER TABLE public.request ADD CONSTRAINT request_curriculum_fk FOREIGN KEY (curriculum_id) REFERENCES public.curriculum(curriculum_id);
ALTER TABLE public.request ADD CONSTRAINT request_account_fk FOREIGN KEY (created_by) REFERENCES public.account(account_id);
ALTER TABLE public.request ADD CONSTRAINT request_major_fk FOREIGN KEY (major_id) REFERENCES public.major(major_id);


ALTER TABLE public.task ADD major_id uuid NULL;
ALTER TABLE public.task ADD CONSTRAINT task_major_fk FOREIGN KEY (major_id) REFERENCES public.major(major_id);
