ALTER TABLE public.review_task ADD comment_material text NULL;
ALTER TABLE public.review_task ADD comment_session text NULL;
ALTER TABLE public.review_task ADD comment_assessment text NULL;
ALTER TABLE public.review_task ADD is_accepted bool NULL;
ALTER TABLE public.review_task DROP COLUMN contents;
