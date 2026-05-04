DROP TABLE public.session_material_block;

ALTER TABLE public."source" ADD source_code varchar(50) NOT NULL;
ALTER TABLE public."source" ADD CONSTRAINT source_unique UNIQUE (source_code);
