CREATE TABLE regulation (
	id uuid PRIMARY KEY NOT NULL,
	code varchar(50) NULL,
	name_regulation varchar(100) NULL,
	description text NULL,
	value INTEGER NULL,
	created_at timestamp NULL
);
