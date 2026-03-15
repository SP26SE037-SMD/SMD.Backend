ALTER TABLE major ADD COLUMN IF NOT EXISTS updated_at timestamp NULL;


CREATE TABLE review_task (
	review_id uuid NOT NULL,
	reviewer_id uuid NOT NULL,
	title_task varchar(50) NULL,
	task_id uuid NOT NULL,
	"content" text NULL,
	review_date timestamp NULL,
	status varchar(50) NULL,
	CONSTRAINT review_task_pk PRIMARY KEY (review_id),
	CONSTRAINT review_task_task_fk FOREIGN KEY (task_id) REFERENCES task(task_id),
	CONSTRAINT review_task_account_fk FOREIGN KEY (reviewer_id) REFERENCES account(account_id)
);
