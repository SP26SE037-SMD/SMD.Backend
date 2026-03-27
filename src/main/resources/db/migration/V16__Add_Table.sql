CREATE TABLE review_task (
	review_id uuid PRIMARY KEY NOT NULL,
	task_id uuid NOT NULL,
	reviewer_id uuid NOT NULL,
	tiltle_task varchar(100) NULL,
	contents text NULL,
	review_date timestamp NULL,
	status varchar(50) NULL,
	CONSTRAINT review_task_account_fk FOREIGN KEY (reviewer_id) REFERENCES account(account_id),
	CONSTRAINT review_task_task_fk FOREIGN KEY (task_id) REFERENCES task(task_id)
);
