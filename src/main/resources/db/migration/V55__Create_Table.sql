CREATE TABLE rubric (
	rubric_id uuid NOT NULL PRIMARY KEY,
	syllabus_id uuid NOT NULL,
	code varchar(50) NOT NULL UNIQUE,
	name varchar(200) NULL,
	description text NULL,
	created_at timestamp NOT NULL,
	CONSTRAINT rubric_syllabus_fk FOREIGN KEY (syllabus_id) REFERENCES syllabus(syllabus_id)
);

CREATE TABLE rubric_criterion (
	criterion_id uuid NOT NULL PRIMARY KEY,
	rubric_id uuid NOT NULL ,
	criterion_name text NULL,
	weight decimal(5, 2) NULL,
	display_order int4 NULL,
	CONSTRAINT rubric_criterion_rubric_fk FOREIGN KEY (rubric_id) REFERENCES rubric(rubric_id)
);

CREATE TABLE rubric_level (
	level_id uuid NOT NULL PRIMARY KEY,
	level_code varchar(30) NOT NULL,
	min_score decimal(4, 1) NULL,
	max_score decimal(4, 1) NULL,
	display_order varchar NULL
);

CREATE TABLE criteria_level (
	id uuid NOT NULL PRIMARY KEY,
	level_id uuid NOT NULL,
	criterion_id uuid NOT NULL,
    description text NULL,
	CONSTRAINT criteria_level_rubric_criterion_fk FOREIGN KEY (criterion_id) REFERENCES rubric_criterion(criterion_id),
	CONSTRAINT criteria_level_rubric_level_fk FOREIGN KEY (level_id) REFERENCES rubric_level(level_id)
);









