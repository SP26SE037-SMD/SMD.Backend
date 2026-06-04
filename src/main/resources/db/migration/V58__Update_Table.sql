ALTER TABLE public.feedback_submissions DROP CONSTRAINT feedback_submissions_curriculum_fk;
ALTER TABLE public.feedback_submissions DROP COLUMN curriculum_id;
ALTER TABLE public.feedback_submissions ADD form_id uuid NULL;
ALTER TABLE public.feedback_submissions ADD CONSTRAINT feedback_submissions_google_form_records_fk FOREIGN KEY (form_id) REFERENCES public.google_form_records(id);
