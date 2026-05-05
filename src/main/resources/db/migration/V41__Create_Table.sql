CREATE TABLE public.task_v2 (
	task_v2_id uuid NOT NULL,
	sprint_id uuid NULL,
	task_name varchar(200) NOT NULL,
	description text NULL,
	assigned_to uuid NULL,
	status varchar(20) DEFAULT 'To Do'::character varying NULL,
	priority varchar(20) NULL,
	due_date timestamp NULL,
	completed_at timestamp NULL,
	created_at timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	task_type varchar(50) NULL,
	target_id uuid NULL,
	root_task_id uuid NULL,
	"action" varchar(100) NULL,
	created_by uuid NULL,
	CONSTRAINT task_v2_pk PRIMARY KEY (task_v2_id),
	CONSTRAINT task_v2_account_fk FOREIGN KEY (assigned_to) REFERENCES public.account(account_id),
	CONSTRAINT task_v2_account_fk_1 FOREIGN KEY (created_by) REFERENCES public.account(account_id),
	CONSTRAINT task_v2_sprint_fk FOREIGN KEY (sprint_id) REFERENCES public.sprint(sprint_id)
);



CREATE TABLE public.review_v2 (
	review_id uuid NOT NULL,
	is_accepted bool NULL,
	"comment" text NULL,
	task_id uuid NULL,
	created_at timestamp NULL,
	CONSTRAINT review_v2_pk PRIMARY KEY (review_id),
	CONSTRAINT review_v2_task_v2_fk FOREIGN KEY (task_id) REFERENCES public.task_v2(task_v2_id)
);

