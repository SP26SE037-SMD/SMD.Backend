CREATE TABLE syllabus_comparison_history
(
    history_id uuid PRIMARY KEY NOT NULL,
    old_syllabus_id uuid NOT NULL,
    new_syllabus_id uuid NOT NULL,
    assessment_diff_json text,
    concept_diff_json text,
    created_at timestamp DEFAULT CURRENT_TIMESTAMP NULL
);
