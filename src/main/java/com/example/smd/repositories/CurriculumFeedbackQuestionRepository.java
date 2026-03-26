package com.example.smd.repositories;

import com.example.smd.entities.Curriculum_Feedback_Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CurriculumFeedbackQuestionRepository extends JpaRepository<Curriculum_Feedback_Question, UUID> {
    List<Curriculum_Feedback_Question> findByFormTypeOrderByQuestionNoAsc(String formType);
}
