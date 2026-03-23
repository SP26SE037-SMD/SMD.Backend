ALTER TABLE public.assessments ALTER COLUMN question_type TYPE varchar(150) USING question_type::varchar(150);
ALTER TABLE public.assessments ALTER COLUMN knowledge_skill TYPE varchar(150) USING knowledge_skill::varchar(150);
