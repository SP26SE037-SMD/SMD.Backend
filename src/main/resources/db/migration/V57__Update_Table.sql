ALTER TABLE public.google_form_records ADD department_id uuid NULL;
ALTER TABLE public.google_form_records ADD CONSTRAINT google_form_records_department_fk FOREIGN KEY (department_id) REFERENCES public.department(department_id);
