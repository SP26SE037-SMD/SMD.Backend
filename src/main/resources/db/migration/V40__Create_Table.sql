CREATE TABLE public.proposed_source (
	id uuid NOT NULL,
	source_id uuid NULL,
	subject_id uuid NULL,
	CONSTRAINT proposed_source_pk PRIMARY KEY (id),
	CONSTRAINT proposed_source_source_fk FOREIGN KEY (source_id) REFERENCES public."source"(source_id),
	CONSTRAINT proposed_source_subjects_fk FOREIGN KEY (subject_id) REFERENCES public.subjects(subject_id)
);
