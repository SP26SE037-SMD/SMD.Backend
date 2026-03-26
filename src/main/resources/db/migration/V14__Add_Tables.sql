CREATE TABLE curriculum_feedback_question (
	question_id uuid PRIMARY KEY NOT NULL,
	question_no int4 NOT NULL,
	question_text text NOT NULL,
	is_required bool NULL,
	form_type varchar(100) NOT NULL,
	question_type varchar(100) NOT NULL,
	created_at timestamp NULL
);
CREATE TABLE "options" (
	option_id uuid PRIMARY KEY NOT NULL,
	option_label text NULL,
	order_index int4 NULL
);
CREATE TABLE feedback_submissions (
	submission_id uuid PRIMARY KEY NOT NULL,
	account_id uuid  NULL,
	curriculum_id uuid NULL,
	created_at timestamp NULL,
	CONSTRAINT feedback_submissions_account_fk FOREIGN KEY (account_id) REFERENCES account(account_id),
	CONSTRAINT feedback_submissions_curriculum_fk FOREIGN KEY (curriculum_id) REFERENCES curriculum(curriculum_id)
);

CREATE TABLE feedback_answers (
	answer_id uuid PRIMARY KEY NOT NULL,
	submission_id uuid NOT NULL,
	question_id uuid NOT NULL,
	selected_option_id uuid NULL,
	answer_text text NULL,
	CONSTRAINT feedback_answers_feedback_submissions_fk FOREIGN KEY (submission_id) REFERENCES feedback_submissions(submission_id),
	CONSTRAINT feedback_answers_curriculum_feedback_question_fk FOREIGN KEY (question_id) REFERENCES curriculum_feedback_question(question_id),
	CONSTRAINT feedback_answers_options_fk FOREIGN KEY (selected_option_id) REFERENCES "options"(option_id)
);



