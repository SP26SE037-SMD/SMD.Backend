ALTER TABLE public.google_form_records ADD description text NULL;
ALTER TABLE public.google_form_records RENAME COLUMN form_type TO form_name;
