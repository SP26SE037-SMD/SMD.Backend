ALTER TABLE feedback_answers
    DROP CONSTRAINT IF EXISTS feedback_answers_curriculum_feedback_question_fk;

ALTER TABLE feedback_answers
    DROP CONSTRAINT IF EXISTS feedback_answers_options_fk;

ALTER TABLE feedback_answers
    ADD CONSTRAINT feedback_answers_feedback_form_question_fk
        FOREIGN KEY (question_id) REFERENCES feedback_form_questions(question_id);

ALTER TABLE feedback_answers
    ADD CONSTRAINT feedback_answers_feedback_form_option_fk
        FOREIGN KEY (selected_option_id) REFERENCES feedback_form_options(option_id);
