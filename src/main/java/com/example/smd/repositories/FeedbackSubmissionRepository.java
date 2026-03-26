package com.example.smd.repositories;

import com.example.smd.entities.FeedbackSubmissions;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FeedbackSubmissionRepository extends JpaRepository<FeedbackSubmissions, UUID> {
    @EntityGraph(attributePaths = {"account", "curriculum", "feedbackAnswers", "feedbackAnswers.question", "feedbackAnswers.selectedOption"})
    @Query("select fs from FeedbackSubmissions fs where fs.id = :id")
    Optional<FeedbackSubmissions> findDetailedById(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"account", "curriculum", "feedbackAnswers", "feedbackAnswers.question", "feedbackAnswers.selectedOption"})
    List<FeedbackSubmissions> findByCurriculum_CurriculumId(UUID curriculumId);

    @EntityGraph(attributePaths = {"account", "curriculum", "feedbackAnswers", "feedbackAnswers.question", "feedbackAnswers.selectedOption"})
    List<FeedbackSubmissions> findByAccount_AccountId(UUID accountId);
}
