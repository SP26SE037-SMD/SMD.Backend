package com.example.smd.repositories;

import com.example.smd.entities.FeedbackFormOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FeedbackFormOptionRepository extends JpaRepository<FeedbackFormOption, UUID> {
    List<FeedbackFormOption> findByQuestion_QuestionIdOrderByOrderIndexAsc(UUID questionId);

    java.util.Optional<FeedbackFormOption> findByQuestion_QuestionIdAndOptionTextIgnoreCase(UUID questionId, String optionText);
}
