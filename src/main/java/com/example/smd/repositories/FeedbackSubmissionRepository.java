package com.example.smd.repositories;

import com.example.smd.entities.FeedbackSubmission;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FeedbackSubmissionRepository extends JpaRepository<FeedbackSubmission, UUID> {
    @EntityGraph(attributePaths = {"account", "curriculum", "feedbackAnswers", "feedbackAnswers.question", "feedbackAnswers.selectedOption"})
    @Query("select fs from FeedbackSubmission fs where fs.id = :id")
    Optional<FeedbackSubmission> findDetailedById(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"account", "curriculum", "feedbackAnswers", "feedbackAnswers.question", "feedbackAnswers.selectedOption"})
    List<FeedbackSubmission> findByCurriculum_CurriculumId(UUID curriculumId);

    @EntityGraph(attributePaths = {"account", "curriculum", "feedbackAnswers", "feedbackAnswers.question", "feedbackAnswers.selectedOption"})
    List<FeedbackSubmission> findByAccount_AccountId(UUID accountId);
}
