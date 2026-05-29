CREATE TABLE public.system_setting (
	id uuid NOT NULL,
	code varchar(50) NOT NULL,
	value text NOT NULL,
	description text NULL,
	CONSTRAINT system_setting_pk PRIMARY KEY (id)
);

INSERT INTO system_setting (id, code, value, description) VALUES
    (gen_random_uuid(), 'SESSION_MINUTE', '50', 'Maximum number of minutes per session'),
    (gen_random_uuid(), 'CREDIT_HOUR', '15', 'The number of study hours per credit')
ON CONFLICT DO NOTHING;
