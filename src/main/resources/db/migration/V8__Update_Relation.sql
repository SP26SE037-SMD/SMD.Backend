DROP TABLE sprint_member;

ALTER TABLE task ADD task_type varchar(50) NULL;

ALTER TABLE review_task RENAME TO request;
ALTER TABLE request RENAME COLUMN review_id TO request_id;
ALTER TABLE request RENAME COLUMN title_task TO title;
ALTER TABLE request ADD requester_id uuid NOT NULL;
ALTER TABLE request ADD CONSTRAINT request_account_fk FOREIGN KEY (requester_id) REFERENCES account(account_id);


ALTER TABLE account ADD department_id uuid NULL;
ALTER TABLE account ADD CONSTRAINT account_department_fk FOREIGN KEY (department_id) REFERENCES department(department_id);
