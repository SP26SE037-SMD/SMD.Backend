ALTER TABLE public.rubric DROP COLUMN description;

ALTER TABLE public.rubric_criterion ADD criteria_code varchar(50) NOT NULL;

